package com.entradasegura.portaria.api.dto;

public record MovimentacaoDTO(
        Long id,
        Long retiradaId,
        String protocolo,
        String alunoNome,
        String turma,
        String responsavelNome,
        String funcionarioNome,
        String dataHora,
        String resultado,
        String justificativa
) {
}
