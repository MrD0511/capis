package com.capis.Commands;

import java.util.ArrayList;
import java.util.List;

import com.capis.DataTpes.Core.ScoreMember;
import com.capis.DataTpes.Core.SortedSetValue;
import com.capis.DataTpes.Core.Value;
import com.capis.DataTpes.RespValues.RespArray;
import com.capis.DataTpes.RespValues.RespBulkString;
import com.capis.DataTpes.RespValues.RespErr;
import com.capis.DataTpes.RespValues.RespValue;
import com.capis.entities.KeyValueStore;

public class ZRANGECommand implements Command {
    private final String name = "ZRANGE";
    private KeyValueStore cache;

    public ZRANGECommand(KeyValueStore cache) {
        this.cache = cache;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public RespValue execute(String[] args) {
        if (args == null || (args.length != 4 && args.length != 5)) {
            return new RespErr("ERR", "wrong number of arguments for 'zrange' command");
        }

        boolean withScores = false;

        if (args.length == 5) {
            if (!args[4].equalsIgnoreCase("WITHSCORES")) {
                return new RespErr("ERR", "syntax error");
            }
            withScores = true;
        }

        Value<?> existing = cache.get(args[1]);

        if (existing == null) {
            return new RespArray(List.of());
        }

        if (!(existing instanceof SortedSetValue zset)) {
            return new RespErr("WRONGTYPE", "Operation against a key holding the wrong kind of value");
        }

        int start;
        int stop;

        try {
            start = Integer.parseInt(args[2]);
            stop = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            return new RespErr("ERR", "value is not an integer or out of range");
        }

        List<ScoreMember> members = new ArrayList<>(zset.getValue());

        if (start < 0) {
            start = members.size() + start;
        }

        if (stop < 0) {
            stop = members.size() + stop;
        }

        start = Math.max(start, 0);
        stop = Math.min(stop, members.size() - 1);

        List<RespValue> result = new ArrayList<>();

        if (start <= stop && start < members.size()) {
            for (int i = start; i <= stop; i++) {
                ScoreMember sm = members.get(i);
                result.add(new RespBulkString(sm.member()));

                if (withScores) {
                    result.add(new RespBulkString(formatScore(sm.score())));
                }
            }
        }

        return new RespArray(result);
    }

    
    private static String formatScore(double score) {
        if (!Double.isInfinite(score) && score == Math.floor(score)) {
            return Long.toString((long) score);
        }
        return Double.toString(score);
    }

}
