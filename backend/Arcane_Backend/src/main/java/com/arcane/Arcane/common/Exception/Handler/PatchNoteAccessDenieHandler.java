package com.arcane.Arcane.common.Exception.Handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Component
public class PatchNoteAccessDenieHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        // 1) 응답 상태를 403으로 설정
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        // 2) 응답의 Content-type를 JSON으로 설정
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 3) JSON 작성
        Map<String, Object> msgMap = new HashMap<>();
        msgMap.put("error", "Access denied - FORBIDDEN");
        msgMap.put("error_description", "이 리소스에 접근할 권한이 없습니다. ADMIN 계정이 아님" );
        msgMap.put("status", HttpStatus.FORBIDDEN.value());

        response.getWriter().write(objectMapper.writeValueAsString(msgMap));
    }
}
