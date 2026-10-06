package com.capis.DataTpes.Core;

public sealed interface Value<T> permits StringValue, ListValue, SetValue, SortedSetValue, HashValue {
    T getValue();

    void setValue(T value);
} 
