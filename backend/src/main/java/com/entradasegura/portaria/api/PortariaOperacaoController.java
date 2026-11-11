package com.entradasegura.portaria.api;

import com.entradasegura.portaria.api.dto.AcoesRetiradaRequests.ConfirmarIdentificacaoRequest;
import com.entradasegura.portaria.api.dto.AcoesRetiradaRequests.RejeitarRequest;
import com.entradasegura.portaria.api.dto.AcoesRetiradaRequests.VersaoRequest;
import com.entradasegura.portaria.api.dto.RetiradaDTO;
import com.entradasegura.portaria.service.PortariaOperacaoService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ações do funcionário sobre uma solicitação. Sem regras de negócio: delega ao
 * {@link PortariaOperacaoService}. Acesso restrito a FUNCIONARIO (PortariaSecurityConfig).
 */
@RestController
@RequestMapping("/api/portaria/retiradas/{id}")
public class PortariaOperacaoController {

    private final PortariaOperacaoService operacoes;

    public PortariaOperacaoController(PortariaOperacaoService operacoes) {
        this.operacoes = operacoes;
    }

    @PostMapping("/iniciar-atendimento")
    public RetiradaDTO iniciarAtendimento(@PathVariable Long id, @RequestBody VersaoRequest req) {
        return operacoes.iniciarAtendimento(id, req.versao());
    }

    @PostMapping("/confirmar-identificacao")
    public RetiradaDTO confirmarIdentificacao(@PathVariable Long id, @RequestBody ConfirmarIdentificacaoRequest req) {
        return operacoes.confirmarIdentificacao(id, req.metodo(), req.versao());
    }

    @PostMapping("/confirmar-retirada")
    public RetiradaDTO confirmarRetirada(@PathVariable Long id, @RequestBody VersaoRequest req) {
        return operacoes.confirmarRetirada(id, req.versao());
    }

    @PostMapping("/rejeitar")
    public RetiradaDTO rejeitar(@PathVariable Long id, @RequestBody RejeitarRequest req) {
        return operacoes.rejeitar(id, req.motivo(), req.justificativa(), req.versao());
    }
}
