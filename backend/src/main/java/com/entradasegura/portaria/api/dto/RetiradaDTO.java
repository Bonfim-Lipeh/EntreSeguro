package com.entradasegura.portaria.api.dto;

import java.util.List;

public record RetiradaDTO(
        Long id,
        String protocolo,
        AlunoResumoDTO aluno,
        ResponsavelSolicitanteDTO responsavel,
        String status,
        String dtSolicitacao,
        String chegadaPrevista,
        String dtInicioAtendimento,
        String dtIdentificacao,
        String dtConclusao,
        FuncionarioResumoDTO atendidoPor,
        String metodoIdentificacao,
        boolean qrCodeValidado,
        String motivoRejeicao,
        String justificativa,
        List<AlertaConferenciaDTO> alertas,
        List<String> acoesPermitidas,
        Long versao
) {
}
