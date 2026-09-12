package com.paytm.assignment.config;
import org.slf4j.MDC;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.*;
import jakarta.servlet.http.*;
import java.util.UUID;
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
                String id = req.getHeader("X-Correlation-Id");
                if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
                MDC.put("correlation_id", id); res.setHeader("X-Correlation-Id", id); return true;
            }
            @Override public void afterCompletion(HttpServletRequest req, HttpServletResponse res, Object h, Exception ex) {
                MDC.remove("correlation_id");
            }
        });
    }
}
