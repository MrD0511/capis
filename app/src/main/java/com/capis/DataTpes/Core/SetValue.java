package com.capis.DataTpes.Core;

import java.util.HashSet;
import java.util.Set;

public final class SetValue implements Value<Set<String>> {
    private Set<String> value;

    public SetValue(Set<String> value) {
        this.value = new HashSet<>(value);
    }

    public Set<String> getValue() {
        return value;
    }

    public void setValue(Set<String> value) {
        this.value = value;
    }
}
