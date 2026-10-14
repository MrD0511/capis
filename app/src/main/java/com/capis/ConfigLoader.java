package com.capis;

import java.util.HashMap;
import java.util.Map;

public class ConfigLoader {

    public static AppConfig load(String[] args) {
        Map<String, String> configMap = parseArgs(args);

        AppConfig.Builder builder = new AppConfig.Builder();

        configMap.forEach((key, value) -> {
            switch (key) {
                case "port" ->
                    builder.setPort(Integer.parseInt(value));
                case "maxConnections" ->
                    builder.setMaxConnections(Integer.parseInt(value));
                case "capacity" ->
                    builder.setCapacity(Integer.parseInt(value));
                case "bufferSize" ->
                    builder.setBufferSize(Integer.parseInt(value));
                case "evictionPolicy" ->
                    builder.setEvictionPolicy(
                        EvictionPolicy.valueOf(value.toUpperCase())
                    );
                case "purgeLimit" ->
                    builder.setPurgeLimit(Integer.parseInt(value));
                default ->
                    throw new IllegalArgumentException(
                        "Unknown configuration option: --" + key
                    );
            }
        });

        return builder.build();
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> configMap = new HashMap<>();

        for (String arg : args) {
            if (!arg.startsWith("--")) {
                throw new IllegalArgumentException(
                    "Invalid argument: " + arg
                );
            }

            int separator = arg.indexOf('=');

            if (separator < 3 || separator == arg.length() - 1) {
                throw new IllegalArgumentException(
                    "Expected --key=value, got: " + arg
                );
            }

            String key = arg.substring(2, separator);
            String value = arg.substring(separator + 1);

            if (configMap.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException(
                    "Duplicate option: --" + key
                );
            }
        }

        return configMap;
    }
}