package com.capis;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfigLoaderTest {

    private final ConfigLoader loader = new ConfigLoader();

    @Test
    void noArgsYieldsDefaults() {
        AppConfig config = loader.load(new String[]{});

        assertEquals(6379, config.getPort());
        assertEquals(100000, config.getCapacity());
        assertEquals(EvictionPolicy.LRU, config.getEvictionPolicy());
    }

    @Test
    void parsesKeyEqualsValuePairs() {
        AppConfig config = loader.load(new String[]{
                "--port=6380",
                "--capacity=50000",
                "--evictionPolicy=LFU",
                "--purgeLimit=64",
                "--bufferSize=8192",
                "--maxConnections=25"
        });

        assertEquals(6380, config.getPort());
        assertEquals(50000, config.getCapacity());
        assertEquals(EvictionPolicy.LFU, config.getEvictionPolicy());
        assertEquals(64, config.getPurgeLimit());
        assertEquals(8192, config.getBufferSize());
        assertEquals(25, config.getMaxConnections());
    }

    @Test
    void evictionPolicyIsCaseInsensitive() {
        AppConfig config = loader.load(new String[]{"--evictionPolicy=lfu"});

        assertEquals(EvictionPolicy.LFU, config.getEvictionPolicy());
    }

    @Test
    void unknownOptionThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> loader.load(new String[]{"--wat=1"}));
    }

    @Test
    void duplicateOptionThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> loader.load(new String[]{"--port=6379", "--port=6380"}));
    }

    @Test
    void argumentWithoutDoubleDashThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> loader.load(new String[]{"port=6379"}));
    }

    @Test
    void argumentWithoutSeparatorThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> loader.load(new String[]{"--port"}));
    }

    @Test
    void argumentWithEmptyValueThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> loader.load(new String[]{"--port="}));
    }

    @Test
    void nonNumericPortThrows() {
        assertThrows(NumberFormatException.class,
                () -> loader.load(new String[]{"--port=abc"}));
    }

    @Test
    void invalidEvictionPolicyThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> loader.load(new String[]{"--evictionPolicy=MRU"}));
    }

    @Test
    void outOfRangePortThrowsFromTheBuilder() {
        assertThrows(IllegalArgumentException.class,
                () -> loader.load(new String[]{"--port=70000"}));
    }
}
