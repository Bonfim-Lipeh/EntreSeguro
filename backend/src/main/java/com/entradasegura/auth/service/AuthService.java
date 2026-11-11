package com.entradasegura.auth.service;

import com.entradasegura.auth.api.dto.AuthResponse;
import com.entradasegura.auth.config.JwtProperties;
import com.entradasegura.auth.domain.RefreshToken;
import com.entradasegura.auth.repository.RefreshTokenRepository;
import com.entradasegura.auth.security.JwtService;
import com.entradasegura.identity.domain.Usuario;
import com.entradasegura.identity.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public AuthService(
            AuthenticationManager authenticationManager,
            UsuarioRepository usuarioRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public AuthResponse login(String email, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas."));

            if (!"ATIVO".equalsIgnoreCase(usuario.getStatus())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário inativo.");
            }

            String accessToken = jwtService.generateAccessToken(usuario);
            String refreshTokenValue = jwtService.generateRefreshTokenValue();
            String refreshTokenHash = jwtService.hashTokenValue(refreshTokenValue);

            RefreshToken refreshToken = new RefreshToken();
            refreshToken.setUsuarioId(usuario.getId());
            refreshToken.setTokenHash(refreshTokenHash);
            refreshToken.setExpiraEm(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpirationMs() / 1000));
            refreshTokenRepository.save(refreshToken);

            return new AuthResponse(accessToken, refreshTokenValue, "Bearer", jwtProperties.getAccessTokenExpirationMs() / 1000);
        } catch (AuthenticationException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.");
        }
    }

    @Transactional
    public AuthResponse refresh(String refreshTokenRaw) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token ausente.");
        }

        String refreshTokenHash = jwtService.hashTokenValue(refreshTokenRaw);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHashAndRevogadoEmIsNull(refreshTokenHash)
                .orElseGet(() -> {
                    if (refreshTokenRepository.findByTokenHash(refreshTokenHash).isPresent()) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido ou revogado.");
                    }
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token inválido ou revogado.");
                });

        if (refreshToken.getExpiraEm().isBefore(LocalDateTime.now())) {
            refreshToken.setRevogadoEm(LocalDateTime.now());
            refreshTokenRepository.save(refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expirado.");
        }

        Usuario usuario = usuarioRepository.findById(refreshToken.getUsuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não encontrado para o refresh token."));

        if (!"ATIVO".equalsIgnoreCase(usuario.getStatus())) {
            refreshToken.setRevogadoEm(LocalDateTime.now());
            refreshTokenRepository.save(refreshToken);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário inativo.");
        }

        String newRefreshValue = jwtService.generateRefreshTokenValue();
        String newRefreshHash = jwtService.hashTokenValue(newRefreshValue);

        refreshToken.setRevogadoEm(LocalDateTime.now());
        refreshToken.setSubstituidoPorHash(newRefreshHash);
        refreshTokenRepository.save(refreshToken);

        RefreshToken nextRefreshToken = new RefreshToken();
        nextRefreshToken.setUsuarioId(usuario.getId());
        nextRefreshToken.setTokenHash(newRefreshHash);
        nextRefreshToken.setExpiraEm(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshTokenExpirationMs() / 1000));
        refreshTokenRepository.save(nextRefreshToken);

        String accessToken = jwtService.generateAccessToken(usuario);
        return new AuthResponse(accessToken, newRefreshValue, "Bearer", jwtProperties.getAccessTokenExpirationMs() / 1000);
    }

    @Transactional
    public void logout(String refreshTokenRaw) {
        if (refreshTokenRaw == null || refreshTokenRaw.isBlank()) {
            return;
        }

        String refreshTokenHash = jwtService.hashTokenValue(refreshTokenRaw);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(refreshTokenHash);
        tokenOpt.ifPresent(token -> {
            token.setRevogadoEm(LocalDateTime.now());
            refreshTokenRepository.save(token);
        });
    }
}
