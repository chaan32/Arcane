package com.arcane.Arcane.common.Filter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class GameNameSanitizerRequestWrapper extends HttpServletRequestWrapper {

    private final String modifiedBody;

    public GameNameSanitizerRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);

        // 원본 JSON Body 읽기
        String body = new BufferedReader(new InputStreamReader(request.getInputStream()))
                .lines()
                .collect(Collectors.joining("\n"));

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.readValue(body, new TypeReference<>() {});

        // "gameName" 필드가 존재하면 공백 제거
        if (map.containsKey("gameName") && map.get("gameName") instanceof String original) {
            // logging
            String trimmed = original.replaceAll(" ", "");
            log.info("[FILTER] 사용자 요청 gameName : {}", original);
            log.info("[FILTER] 공백 제거된 gameName : {}", trimmed);
            map.put("gameName", original.replaceAll(" ", ""));
        }

        // JSON 다시 문자열로 직렬화
        this.modifiedBody = objectMapper.writeValueAsString(map);
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(modifiedBody.getBytes(StandardCharsets.UTF_8));

        return new ServletInputStream() {
            @Override public boolean isFinished() { return byteArrayInputStream.available() == 0; }
            @Override public boolean isReady() { return true; }
            @Override public void setReadListener(ReadListener listener) {}
            @Override public int read() { return byteArrayInputStream.read(); }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(this.getInputStream()));
    }
}