package com.payflow.config;

import com.payflow.security.CorrelationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfig {

    @Bean
    public FilterRegistrationBean<CorrelationFilter> correlationFilterRegistration(CorrelationFilter correlationFilter) {
        FilterRegistrationBean<CorrelationFilter> registrationBean = new FilterRegistrationBean<>(correlationFilter);
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
}