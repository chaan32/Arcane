package com.arcane.Arcane.web.User.dto.request;

import lombok.Data;

@Data
public class UserLoginRequestDto {
    private String loginId;
    private String loginPw;
}
