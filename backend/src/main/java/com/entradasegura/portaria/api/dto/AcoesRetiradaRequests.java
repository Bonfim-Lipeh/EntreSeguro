package com.entradasegura.portaria.api.dto;

/**
 * Corpos das ações do funcionário (contrato em docs/API-PORTARIA.md).
 * Não carregam ids de funcionário nem de escola: esses vêm do JWT.
 */
public final class AcoesRetiradaRequests {

    private AcoesRetiradaRequests() {
    }

    /** iniciar-atendimento e confirmar-retirada. */
    public record VersaoRequest(Long versao) {
    }

    /** confirmar-identificacao: metodo = DOCUMENTO (QR_CODE ainda não disponível). */
    public record ConfirmarIdentificacaoRequest(String metodo, Long versao) {
    }

    /** rejeitar: motivo = MotivoExcecaoRetirada; justificativa obrigatória. */
    public record RejeitarRequest(String motivo, String justificativa, Long versao) {
    }
}
