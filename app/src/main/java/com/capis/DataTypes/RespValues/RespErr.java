package com.capis.DataTpes.RespValues;

public record RespErr(String type, String message) implements RespValue {
}
