package com.banking.api.gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {
    @Bean
    public KeyResolver keyResolver() {
        return exchange -> {
            if (exchange.getRequest().getRemoteAddress() != null
                    && exchange.getRequest()
                    .getRemoteAddress()
                    .getAddress() != null) {

                return Mono.just(
                        exchange.getRequest()
                                .getRemoteAddress()
                                .getAddress()
                                .getHostAddress()
                );
            }

            return Mono.just("unknown");
        };
    }

}