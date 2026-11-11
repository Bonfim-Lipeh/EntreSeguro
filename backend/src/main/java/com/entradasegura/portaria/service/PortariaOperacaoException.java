package com.entradasegura.portaria.service;

import org.springframework.http.HttpStatus;

/**
 * Falha de regra de negócio em uma operação da portaria, com código estável
 * para o frontend (campo {@code codigo} do ProblemDetail).
 */
public class PortariaOperacaoException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;

    public PortariaOperacaoException(HttpStatus status, String codigo, String mensagem) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public static PortariaOperacaoException naoEncontrada() {
        return new PortariaOperacaoException(HttpStatus.NOT_FOUND, "RETIRADA_NAO_ENCONTRADA",
                "Solicitação não encontrada para a escola autorizada.");
    }

    public static PortariaOperacaoException transicaoInvalida(String mensagem) {
        return new PortariaOperacaoException(HttpStatus.CONFLICT, "TRANSICAO_INVALIDA", mensagem);
    }

    public static PortariaOperacaoException versaoDesatualizada() {
        return new PortariaOperacaoException(HttpStatus.CONFLICT, "VERSAO_DESATUALIZADA",
                "A solicitação foi alterada por outra operação. Os dados foram recarregados; confira e tente novamente.");
    }

    public static PortariaOperacaoException autorizacaoNaoVigente() {
        return new PortariaOperacaoException(HttpStatus.CONFLICT, "AUTORIZACAO_EXPIRADA",
                "A autorização deste responsável não está vigente (expirada, revogada ou inexistente). "
                        + "A retirada não pode prosseguir; registre a rejeição com justificativa.");
    }

    public static PortariaOperacaoException validacao(String codigo, String mensagem) {
        return new PortariaOperacaoException(HttpStatus.UNPROCESSABLE_CONTENT, codigo, mensagem);
    }
}
