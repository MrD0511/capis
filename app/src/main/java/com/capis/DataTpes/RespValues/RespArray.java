package com.capis.DataTpes.RespValues;

import java.util.List;

public record RespArray(List<RespValue> values) implements RespValue {
}