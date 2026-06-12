package com.arcane.Arcane.common.Exception.RiotAPI;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class CannotFoundSummoner extends Exception {
    public CannotFoundSummoner(String message) {
        super(message);
    }
}
