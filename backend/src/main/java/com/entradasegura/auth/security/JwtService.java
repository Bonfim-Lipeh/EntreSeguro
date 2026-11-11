package com.entradasegura.auth.security;

import com.entradasegura.auth.config.JwtProperties;
import com.entradasegura.identity.domain.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(Base64.getEncoder()
                .encode(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8)));
    }

    public String generateAccessToken(Usuario usuario) {
        return generateToken(usuario, Duration.ofMillis(jwtProperties.getAccessTokenExpirationMs()));
    }

    public String generateToken(Usuario usuario, Duration expiration) {
        Date now = new Date();
        Date expirationDate = new Date(now.getTime() + expiration.toMillis());

        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(usuario.getEmail())
                .audience().add(jwtProperties.getAudience()).and()
                .claim("usuarioId", usuario.getId())
                .claim("escolaId", usuario.getEscolaId())
                .claim("perfil", usuario.getPerfil().name())
                .claim("nome", usuario.getNome())
                .issuedAt(now)
                .expiration(expirationDate)
                .signWith(signingKey)
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    public Long extractUsuarioId(String token) {
        return getClaims(token).get("usuarioId", Long.class);
    }

    public Long extractEscolaId(String token) {
        return getClaims(token).get("escolaId", Long.class);
    }

    public String extractPerfil(String token) {
        return getClaims(token).get("perfil", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .requireIssuer(jwtProperties.getIssuer())
                .requireAudience(jwtProperties.getAudience())
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String generateRefreshTokenValue() {
        byte[] randomBytes = new byte[32];
        new java.security.SecureRandom().nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public String hashTokenValue(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível para hash de refresh token.", e);
        }
    }
}
