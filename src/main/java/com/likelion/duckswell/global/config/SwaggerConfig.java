package com.likelion.duckswell.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    private static final String GUEST_TOKEN_SCHEME = "guestToken";

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Duckswell API")
                .description("Duckswell 백엔드 API 문서")
                .version("v0.0.1");

        SecurityScheme guestTokenScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .description("POST /api/auth/guest 로 발급받은 guestToken 값을 그대로 입력하세요. (Bearer 접두어 없이)");

        return new OpenAPI()
                .info(info)
                .addSecurityItem(new SecurityRequirement().addList(GUEST_TOKEN_SCHEME))
                .components(new Components().addSecuritySchemes(GUEST_TOKEN_SCHEME, guestTokenScheme));
    }
}
