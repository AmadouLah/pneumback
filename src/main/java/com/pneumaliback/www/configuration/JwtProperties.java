package com.pneumaliback.www.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private long expiration = 86400000; // 24 heures par défaut
    private long refreshExpiration = 604800000; // 7 jours par défaut
}
