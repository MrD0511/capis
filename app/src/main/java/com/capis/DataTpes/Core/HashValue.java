package com.capis.DataTpes.Core;

import java.util.Map;

public final class HashValue implements Value<Map<String, String>> {
    private Map<String, String> value;

    public HashValue(Map<String, String> value) {
        this.value = value;
    }

    @Override
    public Map<String, String> getValue() {
        return value;
    }

    @Override 
    public void setValue(Map<String, String> value) {
        this.value = value;
    }
}
