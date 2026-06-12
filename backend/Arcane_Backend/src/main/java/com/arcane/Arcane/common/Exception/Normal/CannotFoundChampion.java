package com.arcane.Arcane.common.Exception.Normal;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CannotFoundChampion extends RuntimeException {
    public CannotFoundChampion(String message) {
        super(message);
    }
}
