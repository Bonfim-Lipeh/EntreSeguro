package com.entradasegura.auth.config;

import jakarta.annotation.PostConstruct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "entradasegura.jwt")
@Validated
public class JwtProperties {

    @NotBlank(message = "JWT secret deve ser informado por variável de ambiente.")
    private String secret;

    @NotBlank(message = "JWT issuer deve ser informado por variável de ambiente.")
    private String issuer;

    @NotBlank(message = "JWT audience deve ser informado por variável de ambiente.")
    private String audience;

    @NotNull(message = "Tempo de expiração do access token é obrigatório.")
    @Positive(message = "Tempo de expiração do access token deve ser positivo.")
    private Long accessTokenExpirationMs;

    @NotNull(message = "Tempo de expiração do refresh token é obrigatório.")
    @Positive(message = "Tempo de expiração do refresh token deve ser positivo.")
    private Long refreshTokenExpirationMs;

    @PostConstruct
    public void validate() {
        String normalizedSecret = secret == null ? "" : secret.trim();

        if (normalizedSecret.isBlank() || "change-me-in-production".equalsIgnoreCase(normalizedSecret)) {
            throw new IllegalStateException("JWT secret inválido. Configure JWT_SECRET com valor forte e fora do padrão de produção.");
        }

        if (normalizedSecret.length() < 32) {
            throw new IllegalStateException("JWT secret deve ter no mínimo 32 caracteres.");
        }

        if (!normalizedSecret.matches("^[A-Za-z0-9._:-]{32,256}$")) {
            throw new IllegalStateException("JWT secret contém caracteres inválidos para tokens HMAC.");
        }
    }

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public Long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public void setAccessTokenExpirationMs(Long accessTokenExpirationMs) {
        this.accessTokenExpirationMs = accessTokenExpirationMs;
    }

    public Long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public void setRefreshTokenExpirationMs(Long refreshTokenExpirationMs) {
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }
}
