package com.arcane.Arcane.web.User.service;

import com.arcane.Arcane.web.User.dto.request.UserLoginRequestDto;
import com.arcane.Arcane.web.User.dto.request.UserSignUpRequestDto;
import com.arcane.Arcane.common.Auth.jwt.JwtUtil;
import com.arcane.Arcane.common.Exception.Fail.CannotSignUp;
import com.arcane.Arcane.common.Exception.RiotAPI.CannotFoundSummoner;
import com.arcane.Arcane.common.Exception.RiotAPI.IsPresentLoginId;
import com.arcane.Arcane.riot.RiotInform.dto.RiotAccountDto;
import com.arcane.Arcane.riot.RiotInform.service.RiotApiService;
import com.arcane.Arcane.web.User.domain.OAuthAccount;
import com.arcane.Arcane.web.User.domain.OAuthProvider;
import com.arcane.Arcane.web.User.domain.User;
import com.arcane.Arcane.web.User.repository.OAuthAccountRepository;
import com.arcane.Arcane.web.User.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final OAuthAccountRepository oAuthAccountRepository;
    private final RiotApiService riotApiService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public Boolean enrollV1(UserSignUpRequestDto dto){
        User user = User.of(dto, passwordEncoder);
        User save = userRepository.save(user);
        return save!=null;
    }
    public Boolean enrollV2(UserSignUpRequestDto dto) throws IsPresentLoginId {
        // 사전 체크
        if (!checkPossibleByGameNameAndLoginId(dto)) {
            throw new CannotSignUp("아이디 중복 확인 또는 소환사명 인증을 먼저 진행해주세요.");
        }

        // 아이디 중복 예외를 직접 던짐
        if (loginIdCheckV1(dto.getLoginId())) {
            throw new IsPresentLoginId("이미 사용 중인 아이디입니다.");
        }

        // loginId가 null인 경우를 방지하기 위해 추가
        if (dto.getLoginId() == null || dto.getLoginId().isBlank()) {
            throw new CannotSignUp("필수 입력 값이 누락 되었습니다.");
        }

        User user = User.of(dto, passwordEncoder);
        userRepository.save(user);

        return true;
    }

    public Map<String, Object> login(UserLoginRequestDto dto) {
        Optional<User> user0 = userRepository.findByLoginId(dto.getLoginId());
        Map<String, Object> response = new HashMap<>();

        if (!user0.isPresent()) {
            response.put("status", HttpStatus.UNAUTHORIZED.value());
            response.put("message", "Invalid Id");
            response.put("login", false);
            return response;
        }


        User user = user0.get();


        if (user == null
                || user.getLoginPw() == null
                || User.OAUTH_PASSWORD_PLACEHOLDER.equals(user.getLoginPw())
                || !passwordEncoder.matches(dto.getLoginPw(), user.getLoginPw())){
            response.put("status", HttpStatus.UNAUTHORIZED.value());
            response.put("message", "Incorrect login or password");
            response.put("login", false);
            return response;
        }

        // Role로 전달하도록 수정
        String token = jwtUtil.createToken(user.getLoginId(), user.getRole());
        response.put("user_id", user.getId());
        response.put("token", token);
        response.put("message", "Login Success");
        response.put("login", true);
        response.put("nickName", user.getNickName());
        response.put("role", user.getRole().name());
        return response;
    }

    public Boolean loginIdCheckV1(String loginId){
        log.info("loginIdCheck [V1]: loginId={}", loginId);
        return userRepository.findByLoginId(loginId).isPresent();
    }

    public Boolean loginIdCheckV2(String loginId){
        log.info("loginIdCheck [V2]: loginId={}", loginId);
        boolean present = userRepository.findByLoginId(loginId).isPresent();
        try{
            if (present == false){
                return false;
            }
            else throw new IsPresentLoginId("loginId은 중복된 아이디 입니다. ");
        } catch (IsPresentLoginId e){
            return true;
        }
    }

    public RiotAccountDto gameNameCheck(String gameName, String tagLine) throws CannotFoundSummoner {
        return riotApiService.getSummonerInfo(gameName, tagLine);
    }

    public Optional<User> findByLoginId(String loginId) {
        return userRepository.findByLoginId(loginId);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }

        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    @Transactional(readOnly = true)
    public List<OAuthProvider> getConnectedOAuthProviders(User user) {
        return oAuthAccountRepository.findAllByUser(user).stream()
                .map(OAuthAccount::getProvider)
                .toList();
    }

    @Transactional
    public User upsertOAuthUser(OAuthProvider provider, String providerId, String email, String nickName, String profileImage) {
        log.info("OAuth login start: provider={}, email={}", provider, email);

        // 1. 이미 연결된 OAuth 계정인지 먼저 확인한다.
        // 예: GOOGLE + google-sub-123 이 oauth_accounts에 있으면 그 row가 가리키는 User가 실제 로그인 유저다.
        Optional<OAuthAccount> savedOAuthAccount = oAuthAccountRepository.findByProviderAndProviderId(provider, providerId);
        if (savedOAuthAccount.isPresent()) {
            OAuthAccount oAuthAccount = savedOAuthAccount.get();
            oAuthAccount.updateProfile(email, profileImage);

            User user = oAuthAccount.getUser();
            user.updateOAuthProfile(email, nickName, profileImage);
            log.info("OAuth account matched: provider={}, userId={}, onboardingRequired={}", provider, user.getId(), user.isOnboardingRequired());
            return user;
        }

        // 2. 처음 보는 OAuth 계정이면 email로 기존 User를 찾는다.
        // Google과 Naver가 같은 이메일을 내려주면 같은 User에 OAuthAccount만 하나 더 연결된다.
        Optional<User> matchedUserByEmail = findUserByOAuthEmail(email);
        User user;
        if (matchedUserByEmail.isPresent()) {
            user = matchedUserByEmail.get();
            user.updateOAuthProfile(email, nickName, profileImage);
            log.info("OAuth email matched existing user: provider={}, userId={}, email={}", provider, user.getId(), email);
        } else {
            user = userRepository.save(User.oauth(provider, providerId, email, nickName, profileImage));
            log.info("OAuth created new user: provider={}, userId={}, email={}", provider, user.getId(), email);
        }

        // 3. 같은 User에 같은 provider가 이미 연결되어 있으면 providerId가 다른 계정을 덮어쓰지 않는다.
        // 예: 이미 Naver A가 연결된 User에 Naver B가 같은 이메일로 들어오면 데이터가 꼬일 수 있어서 막는다.
        oAuthAccountRepository.findByUserAndProvider(user, provider)
                .ifPresent(existingAccount -> {
                    throw new IllegalStateException("This user already has a different OAuth account for provider: " + provider);
                });

        // 4. OAuth 계정 식별 정보는 users가 아니라 oauth_accounts에 저장한다.
        oAuthAccountRepository.save(OAuthAccount.of(user, provider, providerId, email, profileImage));
        log.info("OAuth account linked: provider={}, userId={}, onboardingRequired={}", provider, user.getId(), user.isOnboardingRequired());
        return user;
    }

    @Transactional
    public User linkOAuthAccount(String loginId, OAuthProvider provider, String providerId, String email, String nickName, String profileImage) {
        if (provider == null || provider == OAuthProvider.LOCAL) {
            throw new IllegalArgumentException("연동할 OAuth provider가 올바르지 않습니다.");
        }

        User currentUser = getCurrentUser(loginId);

        Optional<OAuthAccount> savedOAuthAccount = oAuthAccountRepository.findByProviderAndProviderId(provider, providerId);
        if (savedOAuthAccount.isPresent()) {
            OAuthAccount oAuthAccount = savedOAuthAccount.get();
            if (!oAuthAccount.getUser().getId().equals(currentUser.getId())) {
                return mergeOAuthAccountIntoCurrentUser(currentUser, oAuthAccount, email, nickName, profileImage);
            }

            oAuthAccount.updateProfile(email, profileImage);
            currentUser.updateOAuthProfile(email, nickName, profileImage);
            log.info("OAuth link already matched: provider={}, userId={}", provider, currentUser.getId());
            return currentUser;
        }

        oAuthAccountRepository.findByUserAndProvider(currentUser, provider)
                .ifPresent(existingAccount -> {
                    throw new IllegalStateException("이미 같은 provider가 연결되어 있습니다: " + provider);
                });

        currentUser.updateOAuthProfile(email, nickName, profileImage);
        oAuthAccountRepository.save(OAuthAccount.of(currentUser, provider, providerId, email, profileImage));
        log.info("OAuth linked to current user: provider={}, userId={}", provider, currentUser.getId());
        return currentUser;
    }

    private User mergeOAuthAccountIntoCurrentUser(
            User currentUser,
            OAuthAccount linkedOAuthAccount,
            String email,
            String nickName,
            String profileImage
    ) {
        User sourceUser = linkedOAuthAccount.getUser();
        List<OAuthAccount> sourceOAuthAccounts = oAuthAccountRepository.findAllByUser(sourceUser);

        oAuthAccountRepository.findByUserAndProvider(currentUser, linkedOAuthAccount.getProvider())
                .ifPresent(existingAccount -> {
                    throw new IllegalStateException("현재 계정에 이미 같은 소셜 provider가 연결되어 있습니다: " + linkedOAuthAccount.getProvider());
                });

        linkedOAuthAccount.updateProfile(email, profileImage);
        linkedOAuthAccount.reassignUser(currentUser);
        currentUser.updateOAuthProfile(email, nickName, profileImage);

        for (OAuthAccount sourceOAuthAccount : sourceOAuthAccounts) {
            if (sourceOAuthAccount.getId().equals(linkedOAuthAccount.getId())) {
                continue;
            }

            boolean currentUserAlreadyHasProvider = oAuthAccountRepository
                    .findByUserAndProvider(currentUser, sourceOAuthAccount.getProvider())
                    .isPresent();
            if (!currentUserAlreadyHasProvider) {
                sourceOAuthAccount.reassignUser(currentUser);
            }
        }

        log.info(
                "OAuth accounts merged into current user: sourceUserId={}, targetUserId={}, linkedProvider={}",
                sourceUser.getId(),
                currentUser.getId(),
                linkedOAuthAccount.getProvider()
        );
        return currentUser;
    }

    private Optional<User> findUserByOAuthEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email);
    }

    private boolean checkPossibleByGameNameAndLoginId(UserSignUpRequestDto dto){
        return dto.getIsPresentGameName() == true && dto.getIsPresentId() == false && dto.getIsPresentNickName() == false;
    }

    public boolean checkPossibleUsingNickName(String nickName) {
        return userRepository.findByNickName(nickName).isPresent();
    }

    @Transactional
    public User completeOAuthOnboarding(String loginId, String nickName) {
        if (loginId == null || loginId.isBlank()) {
            throw new IllegalArgumentException("로그인 정보가 없습니다.");
        }
        if (nickName == null || nickName.isBlank()) {
            throw new IllegalArgumentException("별명을 입력해주세요.");
        }

        String normalizedNickName = nickName.trim();
        if (normalizedNickName.length() < 2 || normalizedNickName.length() > 20) {
            throw new IllegalArgumentException("별명은 2자 이상 20자 이하로 입력해주세요.");
        }

        User user = userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        userRepository.findByNickName(normalizedNickName)
                .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                .ifPresent(existingUser -> {
                    throw new IllegalArgumentException("이미 사용 중인 별명입니다.");
                });

        user.completeOnboarding(normalizedNickName);
        return user;
    }

    @Transactional
    public User updateNickName(String loginId, String nickName) {
        return completeOAuthOnboarding(loginId, nickName);
    }
}
