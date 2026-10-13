# Fixing the known limitations

A practical, code-referenced companion to the [Known limitations](../README.md#known-limitations)
section of the README. Every entry below lists the symptom, the root cause with a
`file:line` reference into the current source, and a concrete fix.

Nothing here is implemented yet. Work top-down: the P0 items are small correctness bugs,
P1 items are missing protocol/feature surface, P2 items are structural improvements.

## How to verify a fix

```sh
./gradlew build && ./gradlew test        # 7 parser tests must stay green
./gradlew run                            # then in another terminal:
redis-cli -p 6379 <command>
```

For protocol-level changes (pipelining, inline commands, partial writes) drive the server
with raw sockets or `redis-benchmark`:

```sh
printf '*1\r\n$4\r\nPING\r\n*1\r\n$4\r\nPING\r\n*1\r\n$4\r\nPING\r\n' | nc -q1 localhost 6379
redis-benchmark -p 6379 -t ping_inline,ping_mbulk,set,get -n 100000 -c 50 --csv
```

Before starting: the working tree already contains **uncommitted** edits that remove
`synchronized` from `LRUCache`/`KeyValueStore` and add the missing `size--` on LRU eviction.
Decide whether to keep them (see [B6](#b6-lrucache-locking-and-the-single-threaded-model)),
then commit, so each later fix is its own reviewable diff.

### Priority

| Priority | Items | Why |
| --- | --- | --- |
| P0 | A1–A5, B1–B5, C1–C7, C9 | Wrong answers / dropped data, all small and local |
| P1 | A6, C8, C10, D1 | Missing surface clients expect (`KEYS`, `SCAN`, `CONFIG`, inline, `SET` options, queue-time errors) |
| P2 | B7, D2, D3, D4 | Structural work: zset index, background expiry, `WATCH`, future work |

---

## A. Protocol and server

### A1. Pipelining drops every command after the first

**Symptom.** Three `PING`s written in one `write()` produce a single `PONG`.

**Root cause.** `Parser.decode` returns one command and leaves the rest of the buffer
untouched (`Parser.java:19-36`), but `NioServer.read` calls `CapisCore.handle` exactly once
and then wipes the buffer with `buffer.clear()` (`NioServer.java:87-90`).

**Fix.** Decode and dispatch in a loop, concatenate the replies, and only discard the bytes
actually consumed.

1. Give `CapisCore` a loop (or return a `List<String>`):

```java
// CapisCore.java — replace handle(ByteBuffer, ClientState)
public String handle(ByteBuffer buffer, ClientState clientState) {
    StringBuilder out = new StringBuilder();
    while (buffer.hasRemaining()) {
        String[] command = parser.decode(buffer);
        if (command == null) break;                 // incomplete: keep the rest for the next read
        if (command.length == 0) {
            out.append(parser.encode(new RespErr("ERR", "empty command")));
            continue;
        }
        out.append(parser.encode(commandHandler.execute(command, clientState)));
    }
    return out.toString();
}
```

2. In `NioServer.read`, replace `buffer.clear()` with `buffer.compact()` so unconsumed bytes
   move to the front and the read side continues after them (`NioServer.java:90`).

**Trap.** `decode` returning `null` today means *both* "incomplete" and "malformed". Once you
loop, a malformed command would silently stall the connection forever. Split the outcomes —
make `Parser.decode` return a small sealed type instead of `String[]`:

```java
sealed interface DecodeResult {
    record Complete(String[] command) implements DecodeResult {}
    record Incomplete() implements DecodeResult {
        static final Incomplete INSTANCE = new Incomplete();
    }
    record Malformed(String reason) implements DecodeResult {}
}
```

`Incomplete` → break the loop and compact; `Malformed` → append
`-ERR Protocol error: <reason>` and close the connection (real Redis closes on protocol
errors); `Complete` → dispatch.

**Test.** New `NioServerTest`: connect, write three `PING`s in one buffer, read three
`PONG`s. Also cover `SET`+`GET` pipelined (the existing `parsesPipelinedCommands` test at
`ParserTest.java:104` already proves the parser side).

### A2. Inline commands are rejected

**Symptom.** `PING\r\n` → `-ERR Failed to decode command`; `redis-benchmark`'s
`PING_INLINE` produces no results.

**Root cause.** `Parser.parseElement` switches on the first byte and throws on anything that
is not one of `+ - : $ *` (`Parser.java:168-169`). Inline protocol lines start with a plain
letter.

**Fix.** In `decode`, before `parseElement`, peek the first byte. If it is not a RESP type
byte, parse an inline command: read up to `\r\n`, split on whitespace, return the tokens.

```java
// inside decode, before parseElement — absolute get, does not move position
byte first = buffer.get(buffer.position());
if (!isRespType(first)) {
    String line = readLine(buffer);                  // null -> Incomplete, same as the RESP path
    if (line == null) return DecodeResult.Incomplete.INSTANCE;
    if (line.isBlank()) { /* skip, decode again */ }
    return new DecodeResult.Complete(line.strip().split("\\s+"));
}
// isRespType: '+' '-' ':' '$' '*'
```

Inline lines must bypass the bulk-string/array machinery entirely (they are just text up to
CRLF) but still end up as a `String[]` so `CommandHandler` sees no difference.

Edge cases to handle: an empty line (skip it, don't reply), an inline line longer than the
buffer (protocol error, close), and quoting — Redis does *not* quote, so a plain
`split("\\s+")` with a guard for a leading/trailing space is enough. Inline commands must
also flow through the same `commandHandler.execute` path so `MULTI`/queueing still works.

**Test.** `ParserTest`: `"PING\r\n"` → `["PING"]`; `"SET k v\r\n"` → `["SET","k","v"]`;
a `null` return when the line has no CRLF yet. Re-run the `ping_inline` benchmark case.

### A3. Requests larger than the 8 KB connection buffer are never reassembled

**Symptom.** A single command bigger than `ClientState`'s 8 KB `ByteBuffer`
(`ClientState.java:10`) never completes; the parser reports "Line too long" or stalls.

**Root cause.** One `client.read(buffer)` per readable event (`NioServer.java:76`) and no
growth path: when the buffer fills with a partial command, `readLine` throws
(`Parser.java:201-203`) or `decode` keeps returning `Incomplete`.

**Fix.** Combine with A1's loop:

1. After `client.read(buffer)`, if `decode` says `Incomplete` **and** the buffer is full
   (`!buffer.hasRemaining()` after `compact()`), grow it — allocate a new buffer at 2× and
   copy the pending bytes — up to a hard cap (e.g. 16 MB). Over the cap: reply
   `-ERR Protocol error: too big inline request` and close.
2. `compact()` instead of `clear()` so leftover bytes survive to the next read event.
3. A partial command simply waits for the next `OP_READ`; the selector is level-triggered, so
   no extra wiring is needed.

Store the (resizable) buffer on `ClientState` rather than a fixed field:
`clientState.setInputBuffer(grown)`.

**Test.** Send a 20 KB bulk-string argument in two chunks split mid-payload; assert `SET`
succeeds. Send a 32 MB argument and assert the connection is closed with a protocol error.

### A4. Partial writes are not retried

**Symptom.** Under a slow reader / full socket buffer, responses are silently truncated.

**Root cause.** `sendMessage` does a single `client.write(response)` and ignores the return
value (`NioServer.java:112-119`).

**Fix.** Two options, increasing fidelity:

- **Minimal:** loop until everything is written, selecting on `OP_WRITE` when `write`
  returns `0` so the event loop doesn't spin (you need the `SelectionKey`, so pass it into
  `sendMessage` from `read`):

```java
static void sendMessage(SelectionKey key, String message) throws IOException {
    SocketChannel client = (SocketChannel) key.channel();
    ByteBuffer buf = StandardCharsets.UTF_8.encode(message);
    while (buf.hasRemaining()) {
        if (client.write(buf) == 0) {                 // socket buffer full: wait for writability
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
            key.selector().select();                  // loop resumes when the channel drains
            key.interestOps(SelectionKey.OP_READ);    // (re-apply whatever the base ops are)
        }
    }
}
```

- **Proper:** keep a `Deque<ByteBuffer> pendingWrites` on `ClientState`. `sendMessage`
  enqueues and registers `OP_WRITE`; a new `write(SelectionKey)` branch in `NioServer.start`
  drains it and drops back to `OP_READ` when empty. This also gives you back-pressure: stop
  reading while a large reply is pending.

Note this interacts with A1: one readable event can now produce many replies — enqueue them
all rather than concatenating into one giant string.

**Test.** Hard to unit-test cheaply; a functional test that sets a large value and reads it
back through a socket with a tiny `SO_RCVBUF` is the practical check. At minimum assert the
response byte count matches what the client expected.

### A5. A client can send `$-1` (null) as a command argument

**Symptom.** Only `LPUSH`, `MSET` and `MGET` guard against `null` args
(`LPUSHCommand.java:60`, `MSetCommand.java:36`, `MGetCommand.java:39`); every other command
NPEs and the connection gets `-ERR Internal server error`.

**Root cause.** `Parser.parseElement` happily accumulates `null` for `$-1`
(`Parser.java:136-139`), and nothing centrally rejects it.

**Fix.** Reject once, centrally, instead of per command — in `CommandHandler.execute`
(`CommandHandler.java:27`) before dispatch:

```java
for (String a : args) {
    if (a == null) return new RespErr("ERR", "null argument not allowed");
}
```

Real Redis treats this as a protocol error and drops the connection; either behaviour is
defensible — pick one and document it. Then delete the three per-command `null` guards so
there is a single source of truth.

**Test.** Send `*1\r\n$-1\r\n` and `*3\r\n$3\r\nGET\r\n$-1\r\n`; assert the agreed reply and
that the server is still alive afterwards.

### A6. No `CONFIG` command

**Symptom.** `redis-benchmark` prints `WARNING: Could not fetch server CONFIG`.

**Fix.** Register a `ConfigCommand` in `CapisCore` (`CapisCore.java:59-95`) handling the two
shapes clients probe with:

- `CONFIG GET <pattern>` → `RespArray(List.of())` for unknown patterns (an empty array means
  "no such setting", which is enough to silence the warning), or `["maxmemory","0"]` etc.
- `CONFIG SET ...` → `-ERR CONFIG SET is not supported` (be explicit rather than silent).
- `CONFIG RESETSTAT` / `CONFIG REWRITE` → `-ERR ... not supported`.

Register it as `new ConfigCommand()`; it needs no store handle.

**Test.** `redis-benchmark -t set` no longer prints the warning; `redis-cli config get
maxmemory` returns an empty array.

---

## B. Storage and expiry

### B1. Every plain write gets a hardcoded 24-hour TTL

**Symptom.** `SET k v` then `TTL k` returns `~86400` instead of `-1`.

**Root cause.** `LRUCache.put(K, V)` stamps `now + 24h` (`LRUCache.java:42-44`).

**Fix.** Use the existing sentinel:

```java
public void put(K key, V value) {
    putInternal(key, value, NO_EXPIRY);      // LRUCache.java:142 already treats NO_EXPIRY as "-1"
}
```

Everything else already works: `ttlMillis` returns `-1` for `NO_EXPIRY`
(`LRUCache.java:132-134`), `get` never expires it (`Long.MAX_VALUE`), and `persist` sets the
same sentinel. This also matches Redis semantics — `SET` on an existing key *clears* the TTL,
which `putInternal`'s existing-node branch already does.

Double-check `SETEX`/`EXPIRE` still pass an absolute `expiryAtMillis` via
`KeyValueStore.put(key, value, ttlMillis)` (`KeyValueStore.java:31-33`) — they do, so no
change there.

**Test.** `SET k v; TTL k` → `-1`; `SETEX k 10 v; TTL k` → `10`; `SET k v` over a
`SETEX`-created key → `TTL k` → `-1`.

### B2. Expiry is lazy only

**Symptom.** An expired key keeps occupying an LRU slot until something touches it;
`EXISTS`/`DBSIZE`/`KEYS` can see keys that "should" be gone (they filter, but `size()` and
`remove()` do not — see B3/B4).

**Root cause.** Expiry is checked only in `get`, `containsKey`, `keys` and `expire`
(`LRUCache.java:25, 85, 98, 117`).

**Fix.** Add an explicit sweep and drive it from the event loop so the server stays
single-threaded:

```java
// LRUCache.java  (drop the `size--` if you have already applied B3)
public int purgeExpired() {
    long now = System.currentTimeMillis();
    int removed = 0;
    for (Node<K, V> node : list.nodes()) {          // snapshot, so removal is safe
        if (now > node.expiryAtMillis) {
            list.remove(node);
            map.remove(node.key);
            removed++;
        }
    }
    return removed;
}
```

Call it from `NioServer.start` on a time check between `select()` iterations (e.g. every
5 s: `if (now - lastSweep > 5_000) { capisCore.purgeExpired(); lastSweep = now; }`). A
`ScheduledExecutorService` would be the textbook answer but it introduces a second thread
that races with the unsynchronised cache from B6 — keep it on the loop thread.

Lazy expiry on read must stay as well (it is what makes `GET` correct between sweeps).

**Test.** `SETEX k 1 v; sleep 2; DBSIZE` → `0` without touching `k`; and a loop test that
writes 10 001 short-lived keys and asserts capacity is reclaimed.

### B3. `DBSIZE` is a raw counter

**Symptom.** Not reset by `FLUSHALL`, not decremented by LRU eviction, counts
not-yet-visited expired keys.

**Root cause.** `LRUCache.size` is a hand-maintained `int` (`LRUCache.java:12`) incremented in
`putInternal`, decremented in `get`/`remove` — and **not** in `clear()`
(`LRUCache.java:110-113`) nor (before the current working-tree edit) on eviction.

**Fix.**

1. Delete the `size` field entirely and return `map.size()` from `size()`. The map is already
   the source of truth: `putInternal` keeps it in sync with capacity eviction, and `clear()`
   empties it for free. This removes a whole class of drift bugs (and makes the uncommitted
   `size--` fix unnecessary).
2. Make `size()` purge first: `purgeExpired(); return map.size();` so expired-but-unvisited
   keys are not counted.

`DBSIZECommand` (`DBSIZECommand.java:27`) then needs no change.

**Test.** `FLUSHALL` → `DBSIZE` → `0`; fill past capacity → `DBSIZE` ≤ 10 000; expire keys
without reading them → `DBSIZE` drops.

### B4. `DEL` counts lazily-expired keys as deleted

**Symptom.** `SETEX k 1 v; sleep 2; DEL k` returns `1`.

**Root cause.** `LRUCache.remove` never checks `expiryAtMillis` (`LRUCache.java:73-82`),
while `containsKey` does.

**Fix.** Mirror `containsKey` in `remove`:

```java
public V remove(K key) {
    Node<K, V> node = map.get(key);
    if (node == null) return null;
    if (System.currentTimeMillis() > node.expiryAtMillis) {   // already gone as far as clients care
        list.remove(node); map.remove(key); return null;
    }
    list.remove(node); map.remove(key); return node.value;
}
```

Then `DELCommand`'s `cache.remove(key) != null` check (`DELCommand.java:31`) is correct.
Consider extracting a private `evictIfExpired(K key)` and using it from `get`, `remove`,
`containsKey` and `expire` so the four copies of the rule cannot drift.

**Test.** `SETEX k 1 v; sleep 2; DEL k` → `0`; `EXISTS k` → `0`.

### B5. Capacity is a silent 10 000

**Symptom.** The least-recently-used key vanishes with no signal; capacity is baked into
`App.main` (`App.java:5`).

**Fix (cheap).** Read capacity from an environment variable / first CLI arg in `App` and pass
it through: `new CapisCore(Integer.parseInt(args.length > 0 ? args[0] : "10000"))`. Log the
eviction once (`System.err.println("evicted " + key)`) — but only behind a flag, since
`NioServer` prints per-connection logs anyway.

**Fix (proper, P2).** Expose it through the new `CONFIG` command from A6:
`CONFIG SET maxmemory <bytes>` and `CONFIG GET maxmemory`.

**Test.** Start with capacity `2`, `SET a 1; SET b 2; SET c 3; GET a` → `null`.

### B6. `LRUCache` locking and the single-threaded model

**Symptom (as documented).** Every `LRUCache` method and `KeyValueStore.increment/decrement`
were `synchronized` despite a single-threaded event loop.

**Fix.** Remove the locks — this is exactly what the uncommitted working-tree diff does. Keep
the model honest by writing it down:

- One thread (the `NioServer` loop) owns the store. If you adopt B2's `purgeExpired`, run it
  on that same thread.
- If you ever move to threads (background expiry on an executor, a second accept loop, RESP
  pipelining on a worker pool), re-introduce either `synchronized` around `LRUCache` or a
  `ReadWriteLock`, *and* make in-place mutations (`StringValue.setValue` in
  `KeyValueStore.increment`, `ListValue` mutations in `LPUSH`) atomic — today they are
  unlocked reads-modify-writes of shared objects.

**Test.** No new test; this is a documented invariant. Add a comment on `LRUCache` and a
line in the README's request-lifecycle section.

### B7. Sets are unordered, sorted sets are O(n)

**Symptom.** `SMEMBERS` order is unspecified; `ZADD`/`ZRANGE` are O(n) because
`SortedSetValue.find` linearly scans (`SortedSetValue.java:13-20`) and `ZADD` does
`find` + `removeMember` (another `find`) + `add` per pair (`ZADDCommand.java:56-61`).

**Fix.**

- `SMEMBERS`: leave `HashSet` (Redis order is genuinely unspecified) or switch to
  `LinkedHashSet` in `SADDCommand.java:45` for deterministic tests — cheap and harmless.
- Sorted sets: add a member index beside the `TreeSet`:

```java
public final class SortedSetValue implements Value<SortedSet<ScoreMember>> {
    private SortedSet<ScoreMember> value;
    private final Map<String, ScoreMember> byMember = new HashMap<>();

    public ScoreMember find(String member) { return byMember.get(member); }

    public boolean removeMember(String member) {
        ScoreMember sm = byMember.remove(member);
        return sm != null && value.remove(sm);
    }

    /** @return true if this member was not present before (ZADD counts those). */
    public boolean upsert(String member, double score) {
        ScoreMember next = new ScoreMember(score, member);
        ScoreMember old = byMember.put(member, next);
        if (old != null) value.remove(old);          // rescore: drop the old ordering entry
        value.add(next);
        return old == null;
    }
}
```

Keep the two structures in one class so they cannot diverge, and make `setValue` rebuild the
index. `ZADDCommand` then becomes: parse score → `added += zset.upsert(member, score) ? 1 : 0`
→ return. `ZRANGE` stays O(n) over the range, which is correct.

**Test.** `ZADD k 1 a; ZADD k 1 a` → second returns `0` and `ZRANGE k 0 -1` still has one
element; changing a member's score re-sorts it.

---

## C. Command semantics

### C1. `SADD` returns the set size, not the number added

**Root cause.** `return new RespInteger(set.size());` (`SADDCommand.java:57`).

**Fix.** Count the `add` result:

```java
int added = 0;
for (int i = 2; i < args.length; i++) if (set.add(args[i])) added++;
return new RespInteger(added);
```

Also guard `args[i] == null` here (or centrally, per A5).

**Test.** `SADD k a b a` → `2`; `SCARD k` → `2`; `SADD k a` → `0`.

### C2. `HGETALL` returns nested arrays; `HSET` accepts a bare key

**Root cause.**
- `HGETALLCommand.java:46-48` wraps each pair in its own `RespArray`.
- `HSETCommand.java:28` only checks `args.length % 2 != 0`, so `HSET k` (length 2) passes,
  creates an empty hash and returns `0`.

**Fix.**

```java
// HGETALL — flat, alternating field/value
for (Map.Entry<String, String> e : hash.getValue().entrySet()) {
    result.add(new RespBulkString(e.getKey()));
    result.add(new RespBulkString(e.getValue()));
}
```

```java
// HSET — key + at least one complete pair
if (args == null || args.length < 4 || args.length % 2 != 0) {
    return new RespErr("ERR", "wrong number of arguments for 'HSET' command");
}
```

**Test.** `HSET k f v` → `1`; `HGETALL k` → `[f, v]` flat; `HSET k` → arity error and no key
created (`EXISTS k` → `0`).

### C3. `EXPIRE` arity and error formatting; TTL `0`

**Root cause.**
- Wrong arity returns `null` (`EXPIRECommand.java:25-27`), which encodes as a **null bulk
  string**, not an error.
- `new RespErr("ERR value is not an integer or out of range", name)`
  (`EXPIRECommand.java:34`) puts the whole message in the *type* slot, producing
  `-ERR value is not an integer or out of range EXPIRE`.
- `seconds < 0` is rejected but `0` is accepted (`EXPIRECommand.java:37`).

**Fix.**

```java
if (args == null || args.length != 3) {
    return new RespErr("ERR", "wrong number of arguments for '" + name + "' command");
}
int seconds;
try { seconds = Integer.parseInt(args[2]); }
catch (NumberFormatException e) { return new RespErr("ERR", "value is not an integer or out of range"); }

if (seconds < 0) return new RespErr("ERR", "invalid expire time in '" + name + "' command");
if (seconds == 0) {                                  // Redis: delete the key, report 1 if it existed
    return new RespInteger(cache.remove(args[1]) != null ? 1 : 0);
}
return new RespInteger(cache.expire(args[1], TimeUnit.SECONDS.toMillis(seconds)) ? 1 : 0);
```

`SETEX` mirrors this: `seconds <= 0` → `-ERR invalid expire time in 'SETEX' command`
(`SETEXCommand.java:38-40` currently allows `0`).

**Test.** `EXPIRE k` → arity error; `EXPIRE k abc` → `-ERR value is not an integer or out of
range`; `SET k v; EXPIRE k 0` → `1` and `EXISTS k` → `0`; `SETEX k 0 v` → error.

### C4. `PERSIST` returns 1 for keys that never had a TTL

**Root cause.** `LRUCache.persist` returns `true` for any existing node
(`LRUCache.java:142-151`).

**Fix.**

```java
public boolean persist(K key) {
    Node<K, V> node = map.get(key);
    if (node == null || node.expiryAtMillis == NO_EXPIRY) return false;
    if (System.currentTimeMillis() > node.expiryAtMillis) { /* evict, see B4 */ return false; }
    node.expiryAtMillis = NO_EXPIRY;
    list.moveToFront(node);
    return true;
}
```

This becomes meaningful once B1 makes `NO_EXPIRY` the default for plain writes.

**Test.** `SET k v; PERSIST k` → `0`; `SETEX k 10 v; PERSIST k` → `1; TTL k` → `-1`.

### C5. `INCR`/`DECR` are 32-bit and report wrong types as bad integers

**Root cause.**
- The store computes in `long` (`KeyValueStore.java:58-68`) but the reply is parsed with
  `Integer.parseInt` (`INCRCommand.java:36-38`, `DECRCommand.java:34-36`) and `RespInteger`
  holds an `int` (`RespInteger.java:3`), so values past 2³¹-1 fail and long overflow wraps
  silently.
- `KeyValueStore.increment` throws `IllegalArgumentException` for *both* a wrong-typed key
  (`KeyValueStore.java:51-53`) and a non-integer value (`:59-61`), and the command maps both
  to `value is not an integer or out of range` (`INCRCommand.java:40-45`).

**Fix.**

1. Widen the reply type: `public record RespInteger(long value)`. Every existing
   `new RespInteger(intExpr)` call site widens implicitly, so this is a one-line, no-ripple
   change — verify with `./gradlew build`.
2. `Long.parseLong` / `String.valueOf(long)` in the commands instead of `Integer.parseInt`.
3. Add overflow checks in the store:

```java
if (val == Long.MAX_VALUE) throw new OverflowException();   // "increment or decrement would overflow"
```

4. Split the error types. Introduce two checked exceptions (or a small result type) in
   `KeyValueStore`:

```java
public final class WrongTypeException extends RuntimeException { ... }
public final class NotAnIntegerException extends RuntimeException { ... }
```

and map them in the commands:

```java
} catch (WrongTypeException e) {
    return new RespErr("WRONGTYPE", "Operation against a key holding the wrong kind of value");
} catch (NotAnIntegerException | NumberFormatException e) {
    return new RespErr("ERR", "value is not an integer or out of range");
} catch (OverflowException e) {
    return new RespErr("ERR", "increment or decrement would overflow");
}
```

Also route the in-place `stringValue.setValue(...)` through a store method (e.g.
`cache.put(key, stringValue, keepTtl)`) so the write refreshes recency and (later) bumps the
WATCH version — see D2.

**Test.** `SET k 2147483647; INCR k` → `2147483648`; `SET k 9223372036854775807; INCR k` →
overflow error; `RPUSH k v; INCR k` → `WRONGTYPE`; `SET k abc; INCR k` → `value is not an
integer or out of range`.

### C6. `APPEND` counts characters, `STRLEN` counts bytes

**Root cause.** `AppendCommand.java:33,44` return `String.length()`; `STRLENCommand.java:41`
uses `getBytes(UTF_8).length`.

**Fix.** Make `APPEND` byte-based to match `STRLEN` and Redis:

```java
return new RespInteger(appended.getBytes(StandardCharsets.UTF_8).length);
```

in both branches. (If you instead want "characters", you would also have to change `STRLEN`,
which would then disagree with Redis — don't.)

**Test.** `APPEND k é; STRLEN k` → both `2`; `STRLEN k` on a 3-byte emoji → `3`.

### C7. `LPOP`/`RPOP key <count>` reply shape

**Root cause.** The `count == 1` branches return a bulk string (`LPOPCommand.java:79-87`,
`RPOPCommand.java:78-86`) even when the client explicitly passed a count, and a missing key
with an explicit count returns an empty array (`LPOPCommand.java:61-63`) instead of a nil
array.

**Fix.** Track *whether a count was supplied*, not its value:

```java
boolean withCount = (args.length == 3);
...
if (existing == null) {
    if (!withCount) return null;                 // bulk nil, unchanged
    return RespNullArray.INSTANCE;               // *-1\r\n, see below
}
if (!withCount) { /* existing single-element path, returns RespBulkString */ }
// withCount: always build the array, including the count == 1 case
```

Redis 6.2+ returns a **one-element array** for `LPOP k 1`, so delete the `count == 1` bulk
shortcut when `withCount` is true, and return a nil array (not `[]`) when the key is absent
and a count was given.

`Parser.encode` currently maps Java `null` to `$-1` (`Parser.java:39-41`), so a nil *array*
needs a distinct value type:

```java
public record RespNullArray() implements RespValue {
    public static final RespNullArray INSTANCE = new RespNullArray();
}                                                  // encode -> "*-1\r\n"
```

(Optionally add `RespNullBulk` too and stop relying on Java `null` altogether — see the note
in A1 about `null` meaning several things.)

**Test.** `RPUSH k a b; LPOP k 1` → `*1\r\n$1\r\na\r\n`; `LPOP k` → bulk string;
`LPOP nokey 1` → `*-1`; `LPOP nokey` → `$-1`.

### C8. `SET`, `MSET`, `SETEX` accept no options and reject empty values

**Root cause.** `SetCommand.java:22` hard-requires exactly 3 tokens;
`MSetCommand.java:36` rejects empty keys *and* empty values with a misleading
"wrong number of arguments" error.

**Fix.**

- `MSET`: drop the emptiness check entirely (Redis allows `MSET k ""` and even empty keys);
  keep only the arity check: `args.length < 3 || args.length % 2 == 0`.
- `SET`: parse trailing options instead of rejecting:

```java
// SET key value [NX|XX] [GET] [EX sec|PX ms|EXAT t|PXAT t]
Boolean nx = null, xx = null;                      // null = not specified
boolean get = false;
Long ttlMillis = null;
for (int i = 3; i < args.length; i++) {
    switch (args[i].toUpperCase()) {
        case "NX" -> nx = Boolean.TRUE;
        case "XX" -> xx = Boolean.TRUE;
        case "GET" -> get = Boolean.TRUE;
        case "EX" -> ttlMillis = TimeUnit.SECONDS.toMillis(Long.parseLong(args[++i]));
        case "PX" -> ttlMillis = Long.parseLong(args[++i]);
        default -> { return new RespErr("ERR", "syntax error"); }
    }
}
if (Boolean.TRUE.equals(nx) && Boolean.TRUE.equals(xx)) return new RespErr("ERR", "syntax error");
if (Boolean.TRUE.equals(nx) && cache.containsKey(key)) return null;   // nil reply, key untouched
if (Boolean.TRUE.equals(xx) && !cache.containsKey(key)) return null;
RespValue previous = get ? asBulkOrNull(cache.get(key)) : null;       // read before overwriting

if (ttlMillis == null) cache.put(key, new StringValue(value));        // no TTL (B1)
else                   cache.put(key, new StringValue(value), ttlMillis);  // KeyValueStore takes relative ms

return get ? previous : new RespSimpleString("OK");                   // GET replies with the old value (nil if none)
```

Note `KeyValueStore.put(key, value, ttlMillis)` already converts relative → absolute
(`KeyValueStore.java:31-33`), so pass **relative** milliseconds here, not `now + ttl`.

Wrap `Long.parseLong` in try/catch → `-ERR value is not an integer or out of range`, and
reject `ttlMillis <= 0` with `-ERR invalid expire time in 'SET' command`.

`SETEX` can stay as-is (its own arity error is already correct) — or reimplement it as
`SET` + `EX` internally so the TTL logic lives in one place.

**Test.** `SET k v NX` → nil, key not set; `SET k v2 XX` → `OK`; `SET k v EX 10; TTL k` →
`10`; `SET k ""` → `OK`; `MSET k ""` → `OK`.

### C9. `WRONGTYPE` prefix is inconsistent

**Root cause.** Two spellings are in use:

| Shape | Examples |
| --- | --- |
| `new RespErr("WRONGTYPE", "Operation against ...")` → `-WRONGTYPE ...` | `GetCommand.java:38`, `LPOPCommand.java:68`, `ZRANGECommand.java:50`, set commands |
| `new RespErr("ERR", "WRONGTYPE Operation against ...")` → `-ERR WRONGTYPE ...` | `HSETCommand.java:42`, `HGETALLCommand.java:41`, `ZADDCommand.java:43`, `AppendCommand.java:37`, `STRLENCommand.java:38`, `GETDELCommand.java:36`, `INCRBYFLOATCommand.java:42` |
| bespoke wording | `LPUSHCommand.java:49-52` (`wrong type of value for 'lpush' command`) |

**Fix.** One factory, used everywhere:

```java
// com.capis.DataTpes.RespValues.RespErrors
public final class RespErrors {
    public static RespErr wrongType() {
        return new RespErr("WRONGTYPE", "Operation against a key holding the wrong kind of value");
    }
    public static RespErr wrongArity(String cmd) {
        return new RespErr("ERR", "wrong number of arguments for '" + cmd + "' command");
    }
    public static RespErr notAnInteger() {
        return new RespErr("ERR", "value is not an integer or out of range");
    }
}
```

Mechanical replacement across `Commands/`, plus `LPUSHCommand` adopting `wrongType()`. While
you are there, `wrongArity` fixes the mixed casing (`'lpush'` vs `'EXPIRE'` vs `'get'
command.` with a stray period, `GetCommand.java:24`).

**Test.** A single parameterised test that runs every registered command against a
wrong-typed key and asserts the reply starts with `-WRONGTYPE ` — it will fail today on the
seven `-ERR WRONGTYPE` sites and keep the convention from drifting again.

### C10. No `KEYS` or `SCAN`

**Root cause.** Never registered; `KeyValueStore.keys()` (`KeyValueStore.java:38`) exists and
is unused.

**Fix.** Two new command classes, registered in `CapisCore`:

```java
// KeysCommand — KEYS <pattern>
public RespValue execute(String[] args) {
    if (args.length != 2) return RespErrors.wrongArity("KEYS");
    Pattern p = globToRegex(args[1]);                       // * ? [abc] [^a] escape with \
    List<RespValue> out = new ArrayList<>();
    for (String k : cache.keys()) if (p.matcher(k).matches()) out.add(new RespBulkString(k));
    return new RespArray(out);
}
```

`globToRegex` is ~15 lines (`Pattern.quote` the literal chunks, map `*`→`.*`, `?`→`.`).
Document that `KEYS` is O(n) and blocking — that is exactly what `SCAN` is for:

```java
// ScanCommand — SCAN cursor [MATCH p] [COUNT n]
// cursor is an index into a snapshot of cache.keys()
long cursor = Long.parseLong(args[1]);
List<String> all = new ArrayList<>(cache.keys());           // filtered of expired keys already
int count = 10;                                             // parse COUNT/MATCH options
int from = (int) cursor;
int to = Math.min(from + count, all.size());
List<RespValue> batch = all.subList(from, to).stream().map(RespBulkString::new).toList();
String next = String.valueOf(to >= all.size() ? 0 : to);
return new RespArray(List.of(new RespBulkString(next), new RespArray(batch)));
```

Be honest in the README: this cursor is a snapshot index, not Redis's dict-iterator
guarantee — keys added mid-iteration may be missed. That is acceptable for a learning
project as long as it is documented.

**Test.** `MSET a 1 b 2 c 3; KEYS *` → 3 elements; `KEYS b*` → `[b]`; `SCAN 0 COUNT 1`
repeatedly pages through everything exactly once.

---

## D. Transactions

### D1. No queue-time validation

**Root cause.** `CommandHandler.execute` queues *anything* while a transaction is active
(`CommandHandler.java:56-61`) — unknown commands and wrong arity only surface inside the
`EXEC` reply.

**Fix.** Redis validates at queue time and remembers the failure so `EXEC` returns a nil
array. Three pieces:

1. **Arity metadata.** Add `int arity()` to the `Command` interface
   (`Command.java`): positive = exact argument count, negative = at least `-arity`
   arguments (Redis convention: `SET` → `-3`, `GET` → `2`, `LPUSH` → `-3`). Implement it in
   all 37 classes — a one-liner each.
2. **Validate before queueing:**

```java
default:
    if (transactionManager.isActive(clientState)) {
        if (command == null) {
            clientState.abortTransaction();                  // flag, see below
            return new RespErr("ERR", "Unknown command '" + commandName + "'");
        }
        if (!arityOk(command, args.length)) {
            clientState.abortTransaction();
            return RespErrors.wrongArity(command.getName());
        }
        transactionManager.queue(clientState, args);
        return new RespSimpleString("QUEUED");
    }
```

3. Add `private boolean aborted` to `ClientState` (cleared by `beginTransaction` and
   `endTransaction`). `executeTransaction` returns `RespNullArray` when the flag is set —
   mirroring Redis, which still requires the client to send `EXEC` or `DISCARD`.

Note `MULTI`/`EXEC`/`DISCARD` are handled inline *before* the lookup
(`CommandHandler.java:36-61`), so they keep working; a nested `MULTI` inside a transaction
should be rejected at queue time too (Redis errors with "MULTI calls can not be nested") —
add an explicit case in the queue branch.

**Test.** `MULTI; FOO; EXEC` → `ERR Unknown command` at queue time, `EXEC` → nil array;
`MULTI; GET a b; EXEC` → arity error queued, `EXEC` → nil; `MULTI; SET k v; EXEC` → array
containing `+OK`.

### D2. No `WATCH`/`UNWATCH`

**Root cause.** Never implemented; `TransactionManager` only tracks a queue
(`TransactionManager.java:1-28`).

**Fix.** Optimistic locking needs a version counter per key:

1. `KeyValueStore`: add `private long globalVersion` and `Map<String, Long> keyVersion`.
   Every mutation — `put`, `remove`, `clear`, and the in-place value updates from
   C5 — records `keyVersion.put(key, ++globalVersion)`. Expose:

```java
public long version(String key)       { return keyVersion.getOrDefault(key, -1L); }
public long globalVersion()           { return globalVersion; }
```

   **Trap:** today most mutating commands mutate the `Value` object in place
   (`LPUSHCommand.java:55-68`, `KeyValueStore.increment`, `SADDCommand.java:53-55`) without
   calling `put`. Either add an explicit `cache.touch(key)` call at the end of every mutating
   command, or (better) funnel all mutations through `KeyValueStore` methods so the version
   bump cannot be forgotten. This is the single hardest part of the feature — do it first.

2. `ClientState`: `Map<String, Long> watched = new HashMap<>()` plus the set of watched keys.
   `WATCH k...` records `store.version(k)` for each; `UNWATCH` (and any `EXEC`/`DISCARD`)
   clears the map.

3. `executeTransaction`: before running queued commands, compare each watched key's current
   version to the recorded one. On any mismatch (including "was -1, now exists"), return
   `RespNullArray` and discard the queue without executing anything.

```java
for (Map.Entry<String, Long> w : clientState.getWatched().entrySet()) {
    if (store.version(w.getKey()) != w.getValue()) {
        transactionManager.end(clientState);
        return RespNullArray.INSTANCE;
    }
}
```

Register `WatchCommand`/`UnwatchCommand` in `CapisCore` — they bypass queueing (return
`+OK` immediately even inside `MULTI`, per Redis).

**Test.** The canonical race: connection A `WATCH k; GET k`; connection B `SET k v`;
connection A `MULTI; ...; EXEC` → nil array. And: no concurrent write → `EXEC` succeeds.
Also `UNWATCH` then write then `EXEC` → succeeds.

### D3. Cross-connection isolation

Nothing to change: the single-threaded event loop already makes each *command* atomic, and
`MULTI`/`EXEC` runs on the loop thread so the whole transaction is atomic too. What *would*
break it is introducing threads — see B6. Add one sentence to the README saying isolation is
derived from the single-threaded model, and that it is void if B6's rule is broken.

### D4. No persistence, replication, auth, clustering, blocking commands

Out of scope for the limitation fixes; tracked separately as future work. If you want a
starting point later:

- **Auth:** an `AUTH`/`HELLO` command plus a `boolean authenticated` flag on `ClientState`
  checked at the top of `CommandHandler.execute`; a `requirepass` entry in the new `CONFIG`
  map from A6.
- **Blocking commands (`BLPOP`):** per-key waiter queues in the store; a command that finds
  an empty list registers the `ClientState` as a waiter and returns *no* reply. That needs a
  protocol where "no reply yet" is expressible, so change `CapisCore.handle` to return a
  `RespValue` (or `Optional<String>`) instead of a `String` — do that as part of A1, when you
  are already reshaping that method.
- **Persistence (AOF):** append each successfully executed write command to a log file in the
  event loop, flush on a configurable fsync policy; replay on startup through the same
  dispatcher.

---

## E. Suggested order of work

Each step should be one commit with its tests:

1. **Decide + commit the working-tree changes** (B6, partial B3) and update the README lines
   they invalidate.
2. **Parser contract** — introduce `DecodeResult`, fix the `mark()` clobbering (see below),
   add `RespNullArray`. Tests: `ParserTest` additions.
3. **A1 pipelining + A3 buffer growth + A5 null args** — they all live in the
   `NioServer.read`/`CapisCore.handle` path; do them together. Tests: new `NioServerTest`.
4. **A4 partial writes** (completes the transport work).
5. **B1, B3, B4** (expiry/counter correctness) — store-only, no protocol impact.
6. **C9 error factory**, then the mechanical command fixes **C1–C8**. One commit each, or
   group by data type.
7. **C10 `KEYS`/`SCAN`**, **A6 `CONFIG`**, **A2 inline commands** — feature additions.
8. **D1 queue-time validation** (needs the `arity()` interface change), then **D2 `WATCH`**
   (needs the store version bump).

### A parser bug the README does not mention

`Parser.decode` sets `buffer.mark()` before parsing (`Parser.java:20`) and relies on
`buffer.reset()` to rewind an incomplete command — but `parseElement`, `readLine` and
`readBulkString` all call `mark()` again as they descend (`Parser.java:97, 174, 225`).
`ByteBuffer` has **one** mark, so by the time a nested read fails, the original mark has been
overwritten and `reset()` lands mid-command (typically right after a `$` or `*` byte). The
next decode then starts on a garbage byte, throws `Unknown RESP type byte`, and the command is
lost.

This is why the existing `partialRead` test (`ParserTest.java:81-101`) is weaker than it
looks: it re-wraps the original string instead of asserting the buffer position after a
`null` result, so the bug is invisible.

**Fix (pick one):**

- Track an explicit `int startPos = buffer.position()` at the top of `decode` and call
  `buffer.position(startPos)` on `Incomplete` — no reliance on the mark at all (recommended;
  remove every `mark()`/`reset()` pair from the parse path), **or**
- Use a `java.nio.ByteBuffer` *duplicate* for the speculative parse and only commit
  `buffer.position(...)` when the parse succeeds.

**Test.** `decode` a split command, assert `buffer.position() == 0` after the `null` result,
append the second chunk to the *same* buffer, `flip`, and decode again — this reproduces
exactly what `NioServer` does with `compact()`.

---

## F. Updating the docs afterwards

Once the fixes land, the README needs matching edits (otherwise the "Known limitations"
section becomes wrong in the other direction):

- Remove each fixed bullet; re-count the registered commands if `KEYS`/`SCAN`/`CONFIG`/
  `WATCH`/`UNWATCH` are added (currently "37 registered commands plus …").
- The benchmark notes: `PING_INLINE` should start producing results after A2, and the
  `CONFIG` warning disappears after A6 — update the Notes list and consider re-running the
  table.
- The Features bullet about partial commands being "left in the buffer for the next read" is
  currently overstated (see the parser bug above) — either fix it or soften the claim.
- `README.md:270` references `docs/architecture-upgrade-plan.md`, but `docs/` is empty in the
  working tree. Restore the file or drop the reference (this document was written to
  `docs/` alongside it).
- Add the new invariants: single-threaded store ownership (B6), `WRONGTYPE` helper (C9),
  `arity()` contract on `Command` (D1).
