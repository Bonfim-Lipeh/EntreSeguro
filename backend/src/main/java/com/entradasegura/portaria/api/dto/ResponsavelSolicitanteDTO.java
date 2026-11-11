package com.entradasegura.portaria.api.dto;

import java.time.LocalDateTime;

public record ResponsavelSolicitanteDTO(
        Long id,
        String nome,
        String vinculo,
        String tipoAutorizacao,
        LocalDateTime autorizacaoValidaAte,
        String documentoReferencia
) {
}
