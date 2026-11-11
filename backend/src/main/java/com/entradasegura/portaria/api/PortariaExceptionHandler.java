package com.entradasegura.portaria.api;

import com.entradasegura.portaria.service.PortariaOperacaoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Converte falhas de negócio da portaria em ProblemDetail (RFC 7807) com o campo {@code codigo}. */
@RestControllerAdvice(assignableTypes = {PortariaController.class, PortariaOperacaoController.class})
public class PortariaExceptionHandler {

    @ExceptionHandler(PortariaOperacaoException.class)
    ProblemDetail operacao(PortariaOperacaoException e) {
        return problema(e.getStatus(), e.getCodigo(), e.getMessage());
    }

    /** Duas operações simultâneas sobre a mesma solicitação: a segunda perde no @Version. */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ProblemDetail concorrencia(ObjectOptimisticLockingFailureException e) {
        PortariaOperacaoException v = PortariaOperacaoException.versaoDesatualizada();
        return problema(v.getStatus(), v.getCodigo(), v.getMessage());
    }

    private ProblemDetail problema(HttpStatus status, String codigo, String detalhe) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detalhe);
        problem.setProperty("codigo", codigo);
        return problem;
    }
}
