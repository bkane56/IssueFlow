package com.issueflow.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "issueflow.outbound.worker.enabled", havingValue = "true")
public class OutboundSchedulingConfig {
}
