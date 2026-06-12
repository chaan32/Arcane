package com.arcane.Arcane.common.Exception.Normal;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;


@ResponseStatus(HttpStatus.NOT_FOUND)
public class CannotFoundUser extends Exception {
    public CannotFoundUser(String message) {
        super(message);
    }
}