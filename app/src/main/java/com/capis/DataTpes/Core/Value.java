package com.capis.DataTpes.Core;

public sealed interface Value<T> permits StringValue, ListValue, SetValue, SortedSetValue {
    T getValue();

    void setValue(T value);
} 
