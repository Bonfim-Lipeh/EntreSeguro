package com.entradasegura.pickup.repository;

import com.entradasegura.pickup.domain.MovimentacaoRetirada;
import com.entradasegura.pickup.domain.StatusRetirada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface MovimentacaoRetiradaRepository extends JpaRepository<MovimentacaoRetirada, Long> {

    List<MovimentacaoRetirada> findBySolicitacaoRetiradaIdOrderByDataOcorrenciaAsc(Long solicitacaoRetiradaId);

    List<MovimentacaoRetirada> findByEscolaIdAndStatusNovoInAndDataOcorrenciaGreaterThanEqualOrderByDataOcorrenciaDesc(
            Long escolaId, Collection<StatusRetirada> status, LocalDateTime desde);
}
