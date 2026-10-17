package com.capis;

import java.util.HashMap;
import java.util.Map;

public class ConfigLoader {

    private Map<String, String> configMap;

    public ConfigLoader() {
        this.configMap = new HashMap<>();
    }

    public AppConfig load(String[] args) {
        Map<String, String> configMap = this.parseArgs(args);

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

    private Map<String, String> parseArgs(String[] args) {
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

            if (this.configMap.putIfAbsent(key, value) != null) {
                throw new IllegalArgumentException(
                    "Duplicate option: --" + key
                );
            }
        }

        return this.configMap;
    }
}