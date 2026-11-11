package com.entradasegura.portaria;

import com.entradasegura.portaria.api.PortariaController;
import com.entradasegura.portaria.api.dto.AlunoResumoDTO;
import com.entradasegura.portaria.api.dto.EscolaResumoDTO;
import com.entradasegura.portaria.api.dto.FuncionarioResumoDTO;
import com.entradasegura.portaria.api.dto.MovimentacaoDTO;
import com.entradasegura.portaria.api.dto.ResumoPortariaDTO;
import com.entradasegura.portaria.api.dto.RetiradaDTO;
import com.entradasegura.portaria.api.dto.SessaoPortariaDTO;
import com.entradasegura.portaria.service.PortariaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PortariaControllerTest {

    private MockMvc mockMvc;
    private PortariaService portariaService;

    @BeforeEach
    void setup() {
        portariaService = Mockito.mock(PortariaService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new PortariaController(portariaService)).build();
    }

    @Test
    void deveExibirSessaoDaPortaria() throws Exception {
        SessaoPortariaDTO sessao = new SessaoPortariaDTO(
                new EscolaResumoDTO(1L, "Escola Alpha"),
                new FuncionarioResumoDTO(7L, "Ana Ferreira", "Portaria"),
                "Posto 1"
        );

        when(portariaService.obterSessao()).thenReturn(sessao);

        mockMvc.perform(get("/api/portaria/sessao"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.posto").value("Posto 1"));
    }

    @Test
    void deveListarRetiradasDaFila() throws Exception {
        RetiradaDTO retirada = new RetiradaDTO(
                10L,
                "P-10",
                new AlunoResumoDTO(3L, "João Pedro", "7A"),
                null,
                "AGUARDANDO",
                LocalDateTime.now().toString(),
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                List.of(),
                List.of("INICIAR_ATENDIMENTO"),
                1L
        );

        when(portariaService.listarRetiradas(any(), any())).thenReturn(List.of(retirada));

        mockMvc.perform(get("/api/portaria/retiradas").param("status", "AGUARDANDO").param("busca", "João"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].protocolo").value("P-10"));
    }

    @Test
    void deveObterResumoDaPortaria() throws Exception {
        ResumoPortariaDTO resumo = new ResumoPortariaDTO(1, 2, 3, 4, 5, 6, null);

        when(portariaService.obterResumo()).thenReturn(resumo);

        mockMvc.perform(get("/api/portaria/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aguardando").value(1));
    }

    @Test
    void deveListarMovimentacoesRecentes() throws Exception {
        MovimentacaoDTO movimentacao = new MovimentacaoDTO(
                5L,
                10L,
                "P-10",
                "João Pedro",
                "7A",
                "Maria",
                "Ana",
                LocalDateTime.now().toString(),
                "CONCLUIDA",
                null
        );

        when(portariaService.listarMovimentacoes(anyLong())).thenReturn(List.of(movimentacao));

        mockMvc.perform(get("/api/portaria/movimentacoes").param("limite", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].resultado").value("CONCLUIDA"));
    }
}
