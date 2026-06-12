package com.arcane.Arcane.web.User.dto.request;

import lombok.Data;

@Data
public class UserCheckGameNameRequestDto {
    private String gameName;
    private String tagLine;
}
