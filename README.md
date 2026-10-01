# CAPIS

CAPIS is a small Redis-like in-memory key-value server written in Java. It accepts a subset of Redis Serialization Protocol (RESP) commands over TCP and listens on the default Redis port, `6379`.

## Features

- `PING` returns `PONG`.
- `ECHO <message>` returns the supplied message.
- `SET <key> <value>` stores a value.
- `GET <key>` retrieves a value or returns a null response when the key is missing.
- In-memory LRU storage with a capacity of 100 entries.
- Stored entries expire after 24 hours by default.

This is an educational Redis-like server, not a complete Redis replacement. Data is held in memory and is lost when the process stops.

## Requirements

- Java 26.
- A Unix-like shell for the `./gradlew` commands below. On Windows, use `gradlew.bat` instead.

The Gradle wrapper can resolve the configured Java toolchain when needed.

## Build and test

Run these commands from the repository root:

```sh
./gradlew build
./gradlew test
```

## Run the server

```sh
./gradlew run
```

The server starts on TCP port `6379` and continues running until it is stopped with `Ctrl+C`.

If `redis-cli` is installed, connect to the server in another terminal:

```sh
redis-cli -p 6379 ping
redis-cli -p 6379 set greeting hello
redis-cli -p 6379 get greeting
```

## Project structure

```text
app/src/main/java/com/capis/Server.java              Server entry point
app/src/main/java/com/capis/entities/               RESP parsing and storage classes
gradle/wrapper/                                      Gradle wrapper files
```

## Class diagram

```mermaid
classDiagram
	class App {
		+main(String[] args) void
		-printBanner() void
	}

	class NioServer {
		-int port
		-CapisCore capisCore
		+NioServer(int port, CapisCore capisCore)
		+start() void
		+sendMessage(SocketChannel client, String message) void
	}

	class CapisCore {
		-KeyValueStore keyValueStore
		-Parser parser
		-CommandHandler commandHandler
		+CapisCore(int capacity)
		+run(InputStream input) String
	}

	class Parser {
		+decode(InputStream input) String[]
		+encode(RespValue value) String
		-parseElement(InputStream input, List~String~ accumulator) void
		-readLine(InputStream input) String
		-readBulkString(InputStream input, long length) String
	}

	class CommandHandler {
		-Map~String, Command~ commandMap
		+registerCommand(Command command) void
		+execute(String[] args) RespValue
	}

	class Command {
		<<interface>>
		+getName() String
		+execute(String[] args) RespValue
	}

	class PingCommand
	class EchoCommand
	class GetCommand
	class SetCommand
	class MSetCommand
	class MGetCommand
	class LPUSHCommand
	class RPushCommand
	class LLENCommand
	class LIndexCommand
	class LRangeCommand
	class DELCommand
	class INCRCommand

	class KeyValueStore {
		-LRUCache~String, Value~?~~ cache
		+KeyValueStore(int capacity)
		+get(String key) Value~?~
		+put(String key, Value~?~ value) void
		+remove(String key) Value~?~
		+containsKey(String key) boolean
		+increment(String key) Value~?~
	}

	class LRUCache~K, V~ {
		-DoublyLinkedList~K, V~ list
		-long capacity
		-Map~K, Node~K, V~~ map
		+LRUCache(int capacity)
		+get(K key) V
		+put(K key, V value) void
		+put(K key, V value, long expiryAtMillis) void
		+remove(K key) V
		+containsKey(K key) boolean
		-putInternal(K key, V value, long expiryAtMillis) void
	}

	class DoublyLinkedList~K, V~ {
		-Node~K, V~ head
		-Node~K, V~ tail
		+DoublyLinkedList()
		+addToFront(Node~K, V~ node) void
		+remove(Node~K, V~ node) void
		+removeFromEnd() Node~K, V~
		+moveToFront(Node~K, V~ node) void
	}

	class Node~K, V~ {
		+K key
		+V value
		+long expiryAtMillis
		+Node~K, V~ next
		+Node~K, V~ prev
	}

	class Value~T~ {
		<<sealed interface>>
		+getValue() T
		+setValue(T value) void
	}

	class StringValue
	class ListValue
	class SetValue
	class SortedSetValue

	class RespValue {
		<<interface>>
	}

	class RespArray
	class RespBulkString
	class RespErr
	class RespInteger
	class RespSimpleString

	App --> NioServer : starts
	NioServer *-- CapisCore
	CapisCore *-- KeyValueStore
	CapisCore *-- Parser
	CapisCore *-- CommandHandler
	CommandHandler o-- Command : registers
	CommandHandler --> Command : dispatches
	Command <|.. PingCommand
	Command <|.. EchoCommand
	Command <|.. GetCommand
	Command <|.. SetCommand
	Command <|.. MSetCommand
	Command <|.. MGetCommand
	Command <|.. LPUSHCommand
	Command <|.. RPushCommand
	Command <|.. LLENCommand
	Command <|.. LIndexCommand
	Command <|.. LRangeCommand
	Command <|.. DELCommand
	Command <|.. INCRCommand
	GetCommand --> KeyValueStore
	SetCommand --> KeyValueStore
	MSetCommand --> KeyValueStore
	MGetCommand --> KeyValueStore
	LPUSHCommand --> KeyValueStore
	RPushCommand --> KeyValueStore
	LLENCommand --> KeyValueStore
	LIndexCommand --> KeyValueStore
	LRangeCommand --> KeyValueStore
	DELCommand --> KeyValueStore
	INCRCommand --> KeyValueStore
	Parser --> RespValue : encodes / decodes
	KeyValueStore *-- LRUCache : stores values in
	LRUCache *-- DoublyLinkedList : maintains recency
	LRUCache o-- Node : indexes nodes
	DoublyLinkedList o-- Node : links nodes
	Node --> Node : next / prev
	Value~T~ <|.. StringValue
	Value~T~ <|.. ListValue
	Value~T~ <|.. SetValue
	Value~T~ <|.. SortedSetValue
	RespValue <|.. RespArray
	RespValue <|.. RespBulkString
	RespValue <|.. RespErr
	RespValue <|.. RespInteger
	RespValue <|.. RespSimpleString
	KeyValueStore --> Value~?~
```

Generated Gradle output, compiled classes, IDE metadata, logs, and local environment files are excluded by `.gitignore`.