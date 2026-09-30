package com.capis.DataTpes.Core;

import java.util.SortedSet;
import java.util.TreeSet;

public final class SortedSetValue implements Value<SortedSet<String>> {
    private SortedSet<String> value;

    public SortedSetValue(SortedSet<String> value) {
        this.value = new TreeSet<>(value);
    }

    public SortedSet<String> getValue() {
        return value;
    }

    public void setValue(SortedSet<String> value) {
        this.value = value;
    }
    
}
