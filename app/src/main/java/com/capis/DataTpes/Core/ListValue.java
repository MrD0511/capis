package com.capis.DataTpes.Core;

import java.util.ArrayDeque;
import java.util.Deque;

public final class ListValue implements Value<Deque<String>> {
    private Deque<String> value;

    public ListValue(Deque<String> value) {
        this.value = new ArrayDeque<>(value);
    }

    public Deque<String> getValue() {
        return value;
    }

    public void setValue(Deque<String> value) {
        this.value = new ArrayDeque<>(value);
    }
}
