package com.tiddar.raf.config;


import com.tiddar.raf.http.req.interceptor.AccessLogInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class Interceptors implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AccessLogInterceptor()).addPathPatterns("/**");
    }
}
