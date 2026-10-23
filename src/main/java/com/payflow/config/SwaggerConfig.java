package com.payflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI payflowOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("PayFlow API")
                        .description("PayFlow - Digital Wallet & Payments Backend")
                        .version("1.0.0"));
    }
}