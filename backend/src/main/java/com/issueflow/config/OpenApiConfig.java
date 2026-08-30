package com.issueflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI issueFlowOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("IssueFlow API")
                        .version("1.0")
                        .description("REST API for incident and support issue triage"));
    }
}
