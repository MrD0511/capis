package com.capis;

import com.capis.entities.KeyValueStore;

public class MaintainanceService {
    private final KeyValueStore keyValueStore;
    
    public MaintainanceService(KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    public void maintain() {
        keyValueStore.purgeExpiredEntries();
    }
}
