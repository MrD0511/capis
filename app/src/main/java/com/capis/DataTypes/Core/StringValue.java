package com.capis.DataTpes.Core;

public final class StringValue implements Value<String> {
    private String value;

    public StringValue(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
