package com.arcane.Arcane.web.User.dto.request;

import lombok.Data;

@Data
public class UserSignUpRequestDto {
    private String loginId;
    private String loginPw;
    private String gameName;
    private String tagLine;
    private String puuid;
    private String nickName;
    // false
    private Boolean isPresentId;
    // true
    private Boolean isPresentGameName;
    // false
    private Boolean isPresentNickName;

    @Data
    public static class LoginIdRequestDto {
        private String loginId;
    }

    @Data
    public static class NickNameRequestDto {
        private String nickName;
    }
}
