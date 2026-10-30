package com.capis.DataTypes.RespValues;

public record RespErr(String type, String message) implements RespValue {
}
