package com.arcane.Arcane.monitoring.test.waiting_api;

import com.arcane.Arcane.common.Logging.TraceIds;
import jakarta.servlet.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Slf4j
@Component
public class MdcLoggingFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // 1, 요청이 들어오면 고유한 13글자 Trace Id를 생성
        String traceId = TraceIds.newTraceId();

        // 2. MDC라는 곳에 해당 아ㅣ디를 보관함
        MDC.put("traceId", traceId);

        try{
            // 3. Controller에 요청을 넘겨

            filterChain.doFilter(servletRequest, servletResponse);

        } finally{
            // 요청 처리가 끝나면 반드시 비워줘야 해
            // 톰캣은 재 사용되기 때문에, 안 비우면 다음 사람 로그에 이전 사람 ID가 섞일 수 있어
            MDC.clear();
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}
