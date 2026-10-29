package com.capis.DataTypes.RespValues;

import java.util.List;

public record RespArray(List<RespValue> values) implements RespValue {
}