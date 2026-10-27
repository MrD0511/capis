package com.capis.Config;

import com.capis.DataTypes.EvictionPolicy;


public class AppConfig {
    private final int port;
    private final  int maxConnections;
    private final int capacity;
    private final int bufferSize;
    private final EvictionPolicy evictionPolicy;

    private static final int MIN_PURGE_LIMIT = 16;
    private static final int MAX_PURGE_LIMIT = 256;

    private final int purgeLimit;

    private AppConfig(Builder builder) {
        this.port = builder.port;
        this.maxConnections = builder.maxConnections;
        this.capacity = builder.capacity;
        this.bufferSize = builder.bufferSize;
        this.evictionPolicy = builder.evictionPolicy;
        this.purgeLimit = builder.purgeLimit;
    }

    public int getPort() {
        return port;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public int getPurgeLimit() {
        return purgeLimit;
    }

    public EvictionPolicy getEvictionPolicy() {
        return evictionPolicy;
    }

    public static class Builder {
        private int port = 6379; // Default Redis port
        private int maxConnections = 100; // Default max connections
        private int capacity = 100000; // Default capacity
        private int bufferSize = 16 * 1024; // Default buffer size
        private EvictionPolicy evictionPolicy = EvictionPolicy.LRU; // Default eviction policy
        private int purgeLimit = -1; // Default purge limit, will be calculated based on capacity

        public Builder setPort(int port) {
            if(port < 0 || port > 65535) {
                throw new IllegalArgumentException("Port must be between 0 and 65535");
            }
            this.port = port;
            return this;
        }

        public Builder setMaxConnections(int maxConnections) {
            if(maxConnections < 0 || maxConnections > 100000) {
                throw new IllegalArgumentException("Max connections must be between 0 and 100000");
            }
            this.maxConnections = maxConnections;
            return this;
        }

        public Builder setCapacity(int capacity) {
            if(capacity < 0) {
                throw new IllegalArgumentException("Capacity must be non-negative");
            }
            this.capacity = capacity;
            return this;
        }

        public Builder setBufferSize(int bufferSize) {
            if(bufferSize < 0) {
                throw new IllegalArgumentException("Buffer size must be non-negative");
            }
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder setEvictionPolicy(EvictionPolicy evictionPolicy) {
            if(evictionPolicy == null) {
                throw new IllegalArgumentException("Eviction policy cannot be null");
            }
            this.evictionPolicy = evictionPolicy;
            return this;
        }
        public Builder setPurgeLimit(int purgeLimit) {
            if(purgeLimit < 0) {
                throw new IllegalArgumentException("Purge limit must be non-negative");
            }
            this.purgeLimit = purgeLimit;
            return this;
        }

        public AppConfig build() {
            if(this.purgeLimit == -1){
                this.purgeLimit = calculatePurgeLimit(capacity);
            }

            return new AppConfig(this);
        }

        private  int calculatePurgeLimit(int capacity) {
            return Math.max(
                MIN_PURGE_LIMIT,
                Math.min(MAX_PURGE_LIMIT, capacity / 100)
            );
        }
    }


}
