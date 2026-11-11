package com.entradasegura.portaria.api.dto;

import java.time.LocalDateTime;

public record ResumoPortariaDTO(
        Integer aguardando,
        Integer emAtendimento,
        Integer identificados,
        Integer concluidasHoje,
        Integer rejeitadasHoje,
        Integer canceladasHoje,
        LocalDateTime aguardandoDesde
) {
}
