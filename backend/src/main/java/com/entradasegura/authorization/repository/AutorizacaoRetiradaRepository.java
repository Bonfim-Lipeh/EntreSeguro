package com.entradasegura.authorization.repository;

import com.entradasegura.authorization.domain.AutorizacaoRetirada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AutorizacaoRetiradaRepository extends JpaRepository<AutorizacaoRetirada, Long> {
    Optional<AutorizacaoRetirada> findByEscolaIdAndAlunoIdAndResponsavelId(Long escolaId, Long alunoId, Long responsavelId);
}
