package com.entradasegura.portaria.api;

import com.entradasegura.portaria.api.dto.MovimentacaoDTO;
import com.entradasegura.portaria.api.dto.ResumoPortariaDTO;
import com.entradasegura.portaria.api.dto.RetiradaDTO;
import com.entradasegura.portaria.api.dto.SessaoPortariaDTO;
import com.entradasegura.portaria.service.PortariaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/portaria")
public class PortariaController {

    private final PortariaService portariaService;

    public PortariaController(PortariaService portariaService) {
        this.portariaService = portariaService;
    }

    @GetMapping("/sessao")
    public SessaoPortariaDTO obterSessao() {
        return portariaService.obterSessao();
    }

    @GetMapping("/retiradas")
    public List<RetiradaDTO> listarRetiradas(
            @RequestParam(name = "status", required = false) List<String> status,
            @RequestParam(name = "busca", required = false) String busca
    ) {
        return portariaService.listarRetiradas(status, busca);
    }

    @GetMapping("/retiradas/{id}")
    public RetiradaDTO obterRetirada(@PathVariable Long id) {
        return portariaService.obterRetirada(id);
    }

    @GetMapping("/resumo")
    public ResumoPortariaDTO obterResumo() {
        return portariaService.obterResumo();
    }

    @GetMapping("/movimentacoes")
    public List<MovimentacaoDTO> listarMovimentacoes(
            @RequestParam(name = "limite", defaultValue = "10") Long limite
    ) {
        return portariaService.listarMovimentacoes(limite);
    }
}
