package com.arcane.Arcane.common.Auth;

import com.arcane.Arcane.common.Auth.jwt.JwtAuthenticationFilter;
import com.arcane.Arcane.common.Auth.oauth.CustomOAuth2UserService;
import com.arcane.Arcane.common.Auth.oauth.OAuth2FailureHandler;
import com.arcane.Arcane.common.Auth.oauth.OAuth2SuccessHandler;
import com.arcane.Arcane.common.Exception.Handler.PatchNoteAccessDenieHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;


import java.util.Arrays;
import java.util.List;


@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
    private final PatchNoteAccessDenieHandler patchNoteAccessDenieHandler;

    @Value("${app.cors.allowed-origin-patterns:http://localhost:3000,http://127.0.0.1:3000}")
    private String allowedOriginPatterns;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CustomOAuth2UserService customOAuth2UserService,
            OAuth2SuccessHandler oAuth2SuccessHandler,
            OAuth2FailureHandler oAuth2FailureHandler
    ) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                // OAuth2 로그인은 provider로 이동했다가 다시 돌아오는 동안 state 값을 검증해야 한다.
                // 그래서 OAuth2 콜백 구간에서는 세션이 필요하지만, API 인증 자체는 기존처럼 JWT 필터가 처리한다.
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/api/v1/user/**",
                                "/api/v1/summoner/**",
                                "/import/**",
                                "/api/v1/patchnote/test", "/api/v1/patchnote/find/*", "/api/v1/patchnote/edit/*", "/api/v1/patchnote/delete/*", "/api/v1/patchnote/show/**", "/api/v1/patchnote/riot/**",
                                "/api/v1/comment/**",
                                "/api/v1/champion/**",
                                "/api/v1/rune/**",
                                "/api/v1/summoner-spell/*",
                                "/api/v1/strategy/**",
                                "/api/v1/statistics/**",
                                "/api/v1/analysis/**",
                                "/api/v1/modeling/**",
                                "/api/v1/ranker/**",
                                "/api/v1/ranker/store-rankers/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/v1/championDetail/*",
                                "/api/v1/ranker/automatic/test",
                                "/api/v1/additionalData/iconAndLevel",
                                "/api/v1/tune/**",
                                "/tune.html",
                                "/static/**",
                                "/api/v1/tune/**",
                                "/favicon.ico",
                                "/ws/chat",
                                "/ws/chat/**",
                                "/api/waiting",
                                "/api/waiting/**",
                                "/actuator",
                                "/actuator/**"
                        ).permitAll()
                        .anyRequest().authenticated()
//
                )
                .oauth2Login(oauth -> oauth
                        // Google/Naver에서 받은 사용자 정보를 users 테이블에 저장하거나 갱신한다.
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        // 저장된 User 기준으로 JWT를 발급하고 프론트 콜백 URL로 리다이렉트한다.
                        .successHandler(oAuth2SuccessHandler)
                        // OAuth 로그인 실패 시에도 프론트가 처리할 수 있게 콜백 URL로 돌려보낸다.
                        .failureHandler(oAuth2FailureHandler)
                )
                .exceptionHandling(exception -> exception.accessDeniedHandler(patchNoteAccessDenieHandler)) // patchnote 접근 권한 오류 핸들러
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // CORS 설정
    @Bean
    public CorsConfigurationSource corsConfigurationSource(){
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(parseAllowedOriginPatterns());
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // 모든 경로에 대해 위 설정 적용
        return source;
    }
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/tune.html", "/favicon.ico", "/static/**");
    }

    private List<String> parseAllowedOriginPatterns() {
        return Arrays.stream(allowedOriginPatterns.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }
}
