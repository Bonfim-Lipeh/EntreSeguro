package com.entradasegura.auth.repository;

import com.entradasegura.auth.domain.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    List<RefreshToken> findAllByUsuarioIdAndRevogadoEmIsNull(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshToken> findByTokenHashAndRevogadoEmIsNull(String tokenHash);

    List<RefreshToken> findAllByUsuarioIdAndExpiraEmBefore(Long usuarioId, LocalDateTime instant);
}
