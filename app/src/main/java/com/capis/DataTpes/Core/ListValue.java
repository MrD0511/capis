package com.capis.DataTpes.Core;

import java.util.ArrayList;
import java.util.List;

public final class ListValue implements Value<List<String>> {
    private List<String> value;

    public ListValue(List<String> value) {
        this.value = new ArrayList<>(value);
    }

    public List<String> getValue() {
        return value;
    }

    public void setValue(List<String> value) {
        this.value = value;
    }
}
