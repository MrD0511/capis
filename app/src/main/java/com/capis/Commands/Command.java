package com.capis.Commands;

import com.capis.DataTpes.RespValues.RespValue;

public interface Command {
    String getName();

    RespValue execute(String[] args);
}
