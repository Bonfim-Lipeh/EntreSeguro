package com.entradasegura.people.repository;

import com.entradasegura.people.domain.VinculoAlunoResponsavel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VinculoAlunoResponsavelRepository extends JpaRepository<VinculoAlunoResponsavel, Long> {

    Optional<VinculoAlunoResponsavel> findFirstByEscolaIdAndAlunoIdAndResponsavelIdAndAtivoTrue(
            Long escolaId, Long alunoId, Long responsavelId);
}
