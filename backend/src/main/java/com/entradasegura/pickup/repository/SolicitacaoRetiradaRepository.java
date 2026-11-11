package com.entradasegura.pickup.repository;

import com.entradasegura.pickup.domain.SolicitacaoRetirada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SolicitacaoRetiradaRepository extends JpaRepository<SolicitacaoRetirada, Long> {

    /** Busca restrita à escola do usuário autenticado (isolamento escolar). */
    Optional<SolicitacaoRetirada> findByIdAndEscolaId(Long id, Long escolaId);
}
