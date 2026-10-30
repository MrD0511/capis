package com.capis.Config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.capis.DataTypes.EvictionPolicy;


public class ConfigLoader {

    private final Map<String, String> configMap;
    private static final String CONFIG_FILE_PATH = "config.properties";

    public ConfigLoader() {
        this.configMap = new HashMap<>();
        loadFromFileInternal();
    }

    public void loadFromFile() {
        loadFromFileInternal();
    }

    private void loadFromFileInternal() {

        Properties props = new Properties();

        try (FileInputStream input = new FileInputStream(CONFIG_FILE_PATH)) {
            props.load(input);
            for (String key : props.stringPropertyNames()) {
                this.configMap.put(key, props.getProperty(key));
            }
        } catch (IOException e) {
            // Ignore if config file is not present
        }
    }

    public AppConfig load(String[] args) {
        Map<String, String> parsedConfig = this.parseArgs(args);

        AppConfig.Builder builder = new AppConfig.Builder();

        parsedConfig.forEach((key, value) -> {
            switch (key) {
                case "port" -> builder.setPort(Integer.parseInt(value));
                case "maxConnections" -> builder.setMaxConnections(Integer.parseInt(value));
                case "capacity" -> builder.setCapacity(Integer.parseInt(value));
                case "bufferSize" -> builder.setBufferSize(Integer.parseInt(value));
                case "evictionPolicy" -> builder.setEvictionPolicy(
                    EvictionPolicy.valueOf(value.toUpperCase())
                );
                case "purgeLimit" -> builder.setPurgeLimit(Integer.parseInt(value));
                default -> throw new IllegalArgumentException(
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