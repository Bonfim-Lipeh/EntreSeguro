package com.entradasegura.credential.repository;

import com.entradasegura.credential.domain.CredencialRetirada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CredencialRetiradaRepository extends JpaRepository<CredencialRetirada, Long> {
    List<CredencialRetirada> findAllBySolicitacaoRetiradaId(Long solicitacaoRetiradaId);
}
