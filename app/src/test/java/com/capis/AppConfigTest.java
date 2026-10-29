package com.capis;

import org.junit.jupiter.api.Test;

import com.capis.Config.AppConfig;
import com.capis.DataTypes.EvictionPolicy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppConfigTest {

    @Test
    void defaultsMatchTheDocumentedOnes() {
        AppConfig config = new AppConfig.Builder().build();

        assertEquals(6379, config.getPort());
        assertEquals(100, config.getMaxConnections());
        assertEquals(100000, config.getCapacity());
        assertEquals(16 * 1024, config.getBufferSize());
        assertEquals(EvictionPolicy.LRU, config.getEvictionPolicy());
        assertEquals(256, config.getPurgeLimit());
    }

    @Test
    void builderOverridesDefaults() {
        AppConfig config = new AppConfig.Builder()
                .setPort(6380)
                .setMaxConnections(10)
                .setCapacity(500)
                .setBufferSize(4096)
                .setEvictionPolicy(EvictionPolicy.LFU)
                .setPurgeLimit(32)
                .build();

        assertEquals(6380, config.getPort());
        assertEquals(10, config.getMaxConnections());
        assertEquals(500, config.getCapacity());
        assertEquals(4096, config.getBufferSize());
        assertEquals(EvictionPolicy.LFU, config.getEvictionPolicy());
        assertEquals(32, config.getPurgeLimit());
    }

    @Test
    void purgeLimitIsDerivedFromCapacityAndClamped() {
        assertEquals(16, new AppConfig.Builder().setCapacity(1000).build().getPurgeLimit());
        assertEquals(200, new AppConfig.Builder().setCapacity(20000).build().getPurgeLimit());
        assertEquals(256, new AppConfig.Builder().setCapacity(1_000_000).build().getPurgeLimit());
    }

    @Test
    void explicitPurgeLimitWinsOverTheDerivation() {
        AppConfig config = new AppConfig.Builder()
                .setCapacity(1_000_000)
                .setPurgeLimit(42)
                .build();

        assertEquals(42, config.getPurgeLimit());
    }

    @Test
    void rejectsInvalidPort() {
        assertThrows(IllegalArgumentException.class,
                () -> new AppConfig.Builder().setPort(-1));
        assertThrows(IllegalArgumentException.class,
                () -> new AppConfig.Builder().setPort(65536));
    }

    @Test
    void rejectsInvalidMaxConnections() {
        assertThrows(IllegalArgumentException.class,
                () -> new AppConfig.Builder().setMaxConnections(-1));
        assertThrows(IllegalArgumentException.class,
                () -> new AppConfig.Builder().setMaxConnections(100001));
    }

    @Test
    void rejectsNegativeCapacityAndBufferSizeAndPurgeLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> new AppConfig.Builder().setCapacity(-1));
        assertThrows(IllegalArgumentException.class,
                () -> new AppConfig.Builder().setBufferSize(-1));
        assertThrows(IllegalArgumentException.class,
                () -> new AppConfig.Builder().setPurgeLimit(-1));
    }

    @Test
    void rejectsNullEvictionPolicy() {
        assertThrows(IllegalArgumentException.class,
                () -> new AppConfig.Builder().setEvictionPolicy(null));
    }
}
