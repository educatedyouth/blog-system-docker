package org.hzj.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String secret;
    private Long expirationMs;

    // (必须有 Getters 和 Setters)
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(Long expirationMs) { this.expirationMs = expirationMs; }
}