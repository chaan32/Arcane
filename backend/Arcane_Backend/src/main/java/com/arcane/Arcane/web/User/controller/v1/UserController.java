package com.arcane.Arcane.web.User.controller.v1;

import com.arcane.Arcane.common.Auth.oauth.CustomOAuth2UserService;
import com.arcane.Arcane.common.Logging.ApiLogSupport;
import com.arcane.Arcane.web.User.dto.request.UserCheckGameNameRequestDto;
import com.arcane.Arcane.web.User.dto.request.UserLoginRequestDto;
import com.arcane.Arcane.web.User.dto.request.OAuthOnboardingRequestDto;
import com.arcane.Arcane.web.User.dto.request.UserSignUpRequestDto;
import com.arcane.Arcane.common.Exception.Fail.SignUpFail;
import com.arcane.Arcane.common.Exception.RiotAPI.CannotFoundSummoner;
import com.arcane.Arcane.common.Exception.RiotAPI.IsPresentLoginId;
import com.arcane.Arcane.riot.RiotInform.dto.RiotAccountDto;
import com.arcane.Arcane.web.User.domain.OAuthProvider;
import com.arcane.Arcane.web.User.domain.User;
import com.arcane.Arcane.web.User.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;


    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody UserLoginRequestDto userLoginRequestDto) throws ChangeSetPersister.NotFoundException {
        // 로그인 하기
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "user",
                "login",
                "START",
                "loginId=" + userLoginRequestDto.getLoginId()
        );

        Map<String, Object> response = userService.login(userLoginRequestDto);


        if (response.get("login").toString().equals("true")){
            log.info(ApiLogSupport.BUSINESS_FLOW, "user", "login", "SUCCESS", "loginId=" + userLoginRequestDto.getLoginId());
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        log.warn(ApiLogSupport.BUSINESS_FLOW, "user", "login", "FAILED", "loginId=" + userLoginRequestDto.getLoginId());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    @PostMapping("/check/nickName")
    public ResponseEntity<Map<String, Boolean>> nickNameCheck(@RequestBody UserSignUpRequestDto.NickNameRequestDto dto){
        boolean b = userService.checkPossibleUsingNickName(dto.getNickName());

        Map<String, Boolean> response = new HashMap<>();
        response.put("isPresentNickName", b);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
    @PostMapping("/check/loginId")
    public ResponseEntity<Map<String, Boolean>> loginIdCheck(@RequestBody UserSignUpRequestDto.LoginIdRequestDto dto){
        // 로그인 아이디 중복 체크
        boolean isPresent = userService.loginIdCheckV1(dto.getLoginId());
//        boolean isPresent = userService.loginIdCheckV2(dto.getLoginId());
        log.info(ApiLogSupport.BUSINESS_FLOW, "user", "checkLoginId", "SUCCESS", "loginId=" + dto.getLoginId() + ", duplicated=" + isPresent);

        Map<String, Boolean> response = new HashMap<>();
        response.put("isPresentId", isPresent);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/check/gameName")
    public ResponseEntity<Map<String, Object>> gameNameCheck(@RequestBody UserCheckGameNameRequestDto dto) throws CannotFoundSummoner {
        // 소환사 이름 존재 여부 체크 -> 라이엇 API에 요청을 해서 존재 여부를 파악 필요 함
        log.info(ApiLogSupport.BUSINESS_FLOW, "user", "checkGameName", "START", "gameName=" + dto.getGameName() + ", tagLine=" + dto.getTagLine());

        Map<String, Object> response = new HashMap<>();

        RiotAccountDto isPresentGameName = userService.gameNameCheck(dto.getGameName(), dto.getTagLine());

        if (isPresentGameName != null){
            response.put("isPresentNickName", "true");
            response.put("puuid", isPresentGameName.getPuuid());
            response.put("gameName", dto.getGameName());
            response.put("tagLine", dto.getTagLine());
        }
        else response.put("isPresentGameName", "false");
        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "user",
                "checkGameName",
                "SUCCESS",
                "gameName=" + dto.getGameName() + ", tagLine=" + dto.getTagLine() + ", exists=" + (isPresentGameName != null)
        );

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/signup")
    public ResponseEntity<Map<String, Object>> signup (@RequestBody UserSignUpRequestDto dto) throws IsPresentLoginId {

        if (dto.getIsPresentGameName() == null || dto.getIsPresentId() == null || dto.getIsPresentNickName() == null){
            throw new SignUpFail("아이디 중복 확인, 소환사명 인증 혹은 닉네임 중복 확인을 먼저 진행 해주세요.");
        }

        log.info(
                ApiLogSupport.BUSINESS_FLOW,
                "user",
                "signup",
                "START",
                "loginId=" + dto.getLoginId() + ", nickName=" + dto.getNickName() + ", gameName=" + dto.getGameName()
        );
        Boolean enrollResult = userService.enrollV2(dto);

        Map<String, Object> response = new HashMap<>();
        if (enrollResult){
            response.put("enroll", "success");
        }
        else {
            response.put("enroll", "fail");
        }
        log.info(ApiLogSupport.BUSINESS_FLOW, "user", "signup", enrollResult ? "SUCCESS" : "FAILED", "loginId=" + dto.getLoginId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/onboarding")
    public ResponseEntity<Map<String, Object>> completeOAuthOnboarding(
            @AuthenticationPrincipal String loginId,
            @RequestBody OAuthOnboardingRequestDto dto
    ) {
        if (loginId == null || loginId.isBlank()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "로그인이 필요합니다.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        try {
            User user = userService.completeOAuthOnboarding(loginId, dto.getNickName());

            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());
            response.put("loginId", user.getLoginId());
            response.put("nickName", user.getNickName());
            response.put("role", user.getRole().name());
            response.put("onboardingCompleted", !user.isOnboardingRequired());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(@AuthenticationPrincipal String loginId) {
        if (loginId == null || loginId.isBlank()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "로그인이 필요합니다.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        try {
            User user = userService.getCurrentUser(loginId);
            List<String> connectedProviders = userService.getConnectedOAuthProviders(user).stream()
                    .map(provider -> provider.name().toLowerCase())
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());
            response.put("loginId", user.getLoginId());
            response.put("nickName", user.getNickName());
            response.put("email", user.getEmail());
            response.put("profileImage", user.getProfileImage());
            response.put("connectedProviders", connectedProviders);
            response.put("onboardingCompleted", !user.isOnboardingRequired());
            response.put("role", user.getRole().name());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
    }

    @PatchMapping("/me/nickname")
    public ResponseEntity<Map<String, Object>> updateNickName(
            @AuthenticationPrincipal String loginId,
            @RequestBody OAuthOnboardingRequestDto dto
    ) {
        if (loginId == null || loginId.isBlank()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "로그인이 필요합니다.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        try {
            User user = userService.updateNickName(loginId, dto.getNickName());

            Map<String, Object> response = new HashMap<>();
            response.put("userId", user.getId());
            response.put("loginId", user.getLoginId());
            response.put("nickName", user.getNickName());
            response.put("role", user.getRole().name());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        }
    }

    @PostMapping("/oauth/link-intent/{provider}")
    public ResponseEntity<Map<String, Object>> createOAuthLinkIntent(
            @AuthenticationPrincipal String loginId,
            @PathVariable String provider,
            HttpSession session
    ) {
        if (loginId == null || loginId.isBlank()) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "로그인이 필요합니다.");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        OAuthProvider oAuthProvider;
        try {
            oAuthProvider = OAuthProvider.valueOf(provider.toUpperCase());
            if (oAuthProvider == OAuthProvider.LOCAL) {
                throw new IllegalArgumentException("LOCAL은 OAuth provider가 아닙니다.");
            }
        } catch (IllegalArgumentException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("message", "지원하지 않는 소셜 로그인입니다.");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        session.setAttribute(CustomOAuth2UserService.OAUTH_LINK_LOGIN_ID_SESSION_KEY, loginId);
        session.setAttribute(CustomOAuth2UserService.OAUTH_LINK_PROVIDER_SESSION_KEY, oAuthProvider.name());

        Map<String, Object> response = new HashMap<>();
        response.put("authorizationUrl", "/oauth2/authorization/" + oAuthProvider.name().toLowerCase());
        return ResponseEntity.ok(response);
    }
}
