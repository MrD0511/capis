package com.capis.Commands;

import com.capis.DataTypes.RespValues.RespValue;

public interface Command {
    String getName();

    default boolean changesState(){
        return false;
    };

    RespValue execute(String[] args);

    default RespValue persistentCommand(String[] args){ return null; };
}
