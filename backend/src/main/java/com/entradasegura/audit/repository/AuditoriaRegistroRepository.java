package com.entradasegura.audit.repository;

import com.entradasegura.audit.domain.AuditoriaRegistro;
import org.springframework.data.repository.Repository;

import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public interface AuditoriaRegistroRepository extends Repository<AuditoriaRegistro, Long>, AuditoriaRegistroRepositoryCustom {
    AuditoriaRegistro save(AuditoriaRegistro auditoriaRegistro);

    Optional<AuditoriaRegistro> findById(Long id);

    List<AuditoriaRegistro> findAll();

    long count();
}

interface AuditoriaRegistroRepositoryCustom {
    AuditoriaRegistro saveAndFlush(AuditoriaRegistro auditoriaRegistro);
}
