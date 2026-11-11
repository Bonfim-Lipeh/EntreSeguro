package com.entradasegura.audit.repository;

import com.entradasegura.audit.domain.AuditoriaRegistro;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AuditoriaRegistroRepositoryImpl implements AuditoriaRegistroRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public AuditoriaRegistro saveAndFlush(AuditoriaRegistro auditoriaRegistro) {
        AuditoriaRegistro salvo = entityManager.merge(auditoriaRegistro);
        entityManager.flush();
        return salvo;
    }
}
