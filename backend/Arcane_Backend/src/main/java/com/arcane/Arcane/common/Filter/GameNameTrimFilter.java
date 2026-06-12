package com.arcane.Arcane.common.Filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(1)
public class GameNameTrimFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // 회원가입 요청은 제외
        if (path.contains("/signup")) {
            // logging
            System.out.println("[FILTER] /signup 요청은 필터 적용 제외됨");
            chain.doFilter(request, response);
            return;
        }

        // JSON 요청에서만 필터 적용
        String contentType = httpRequest.getContentType();
        if (contentType != null && contentType.contains("application/json")) {
            GameNameSanitizerRequestWrapper wrapper = new GameNameSanitizerRequestWrapper(httpRequest);
            chain.doFilter(wrapper, response);
        } else {
            chain.doFilter(request, response);
        }
    }
}