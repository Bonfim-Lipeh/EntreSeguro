package com.entradasegura.pickup.service;

import com.entradasegura.audit.domain.AuditoriaRegistro;
import com.entradasegura.audit.repository.AuditoriaRegistroRepository;
import com.entradasegura.pickup.domain.MotivoExcecaoRetirada;
import com.entradasegura.pickup.domain.MovimentacaoRetirada;
import com.entradasegura.pickup.domain.SolicitacaoRetirada;
import com.entradasegura.pickup.domain.StatusRetirada;
import com.entradasegura.pickup.repository.MovimentacaoRetiradaRepository;
import com.entradasegura.pickup.repository.SolicitacaoRetiradaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class SolicitacaoRetiradaTransitionService {

    private final SolicitacaoRetiradaRepository solicitacaoRetiradaRepository;
    private final MovimentacaoRetiradaRepository movimentacaoRetiradaRepository;
    private final AuditoriaRegistroRepository auditoriaRegistroRepository;

    public SolicitacaoRetiradaTransitionService(
            SolicitacaoRetiradaRepository solicitacaoRetiradaRepository,
            MovimentacaoRetiradaRepository movimentacaoRetiradaRepository,
            AuditoriaRegistroRepository auditoriaRegistroRepository) {
        this.solicitacaoRetiradaRepository = solicitacaoRetiradaRepository;
        this.movimentacaoRetiradaRepository = movimentacaoRetiradaRepository;
        this.auditoriaRegistroRepository = auditoriaRegistroRepository;
    }

    @Transactional
    public SolicitacaoRetirada iniciarAtendimento(Long solicitacaoId, Long funcionarioId, String observacao, String motivoOperacao) {
        return iniciarAtendimento(solicitacaoId, funcionarioId, observacao, motivoOperacao, motivoOperacao);
    }

    @Transactional
    public SolicitacaoRetirada iniciarAtendimento(Long solicitacaoId, Long funcionarioId, String observacao, String motivoOperacao, String motivoAuditoria) {
        SolicitacaoRetirada solicitacao = carregarSolicitacao(solicitacaoId);
        solicitacao.iniciarAtendimento(funcionarioId, observacao);
        return persistirTransicao(
                solicitacao,
                funcionarioId,
                null,
                StatusRetirada.AGUARDANDO,
                StatusRetirada.EM_ATENDIMENTO,
                "INICIAR_ATENDIMENTO",
                "FUNCIONARIO",
                motivoOperacao,
                observacao,
                null,
                motivoAuditoria
        );
    }

    @Transactional
    public SolicitacaoRetirada confirmarIdentificacao(Long solicitacaoId, Long funcionarioId, String observacao, String motivoOperacao) {
        SolicitacaoRetirada solicitacao = carregarSolicitacao(solicitacaoId);
        solicitacao.confirmarIdentificacao(funcionarioId, observacao);
        return persistirTransicao(
                solicitacao,
                funcionarioId,
                null,
                StatusRetirada.EM_ATENDIMENTO,
                StatusRetirada.IDENTIFICADO,
                "CONFIRMAR_IDENTIFICACAO",
                "FUNCIONARIO",
                motivoOperacao,
                observacao,
                null,
                motivoOperacao
        );
    }

    @Transactional
    public SolicitacaoRetirada rejeitar(
            Long solicitacaoId,
            Long funcionarioId,
            String justificativa,
            MotivoExcecaoRetirada motivoExcecao,
            String observacao,
            String motivoOperacao) {
        SolicitacaoRetirada solicitacao = carregarSolicitacao(solicitacaoId);
        // A rejeição pode partir de EM_ATENDIMENTO ou, por autorização expirada, de IDENTIFICADO.
        StatusRetirada statusAnterior = solicitacao.getStatus();
        solicitacao.rejeitar(funcionarioId, justificativa, motivoExcecao, observacao);
        return persistirTransicao(
                solicitacao,
                funcionarioId,
                null,
                statusAnterior,
                StatusRetirada.REJEITADA,
                "REJEITAR",
                "FUNCIONARIO",
                motivoOperacao,
                observacao,
                justificativa,
                motivoOperacao
        );
    }

    @Transactional
    public SolicitacaoRetirada confirmarRetirada(Long solicitacaoId, Long funcionarioId, String observacao, String motivoOperacao) {
        SolicitacaoRetirada solicitacao = carregarSolicitacao(solicitacaoId);
        solicitacao.confirmarRetirada(funcionarioId, observacao);
        return persistirTransicao(
                solicitacao,
                funcionarioId,
                null,
                StatusRetirada.IDENTIFICADO,
                StatusRetirada.CONCLUIDA,
                "CONFIRMAR_RETIRADA",
                "FUNCIONARIO",
                motivoOperacao,
                observacao,
                null,
                motivoOperacao
        );
    }

    @Transactional
    public SolicitacaoRetirada cancelarPorResponsavel(Long solicitacaoId, Long responsavelId, String motivo, String observacao, String motivoOperacao) {
        SolicitacaoRetirada solicitacao = carregarSolicitacao(solicitacaoId);
        solicitacao.cancelarPorResponsavel(responsavelId, motivo, observacao);
        return persistirTransicao(
                solicitacao,
                null,
                responsavelId,
                StatusRetirada.AGUARDANDO,
                StatusRetirada.CANCELADA,
                "CANCELAR",
                "RESPONSAVEL",
                motivoOperacao,
                observacao,
                null,
                motivoOperacao
        );
    }

    private SolicitacaoRetirada carregarSolicitacao(Long solicitacaoId) {
        return solicitacaoRetiradaRepository.findById(solicitacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação de retirada não encontrada: " + solicitacaoId));
    }

    private SolicitacaoRetirada persistirTransicao(
            SolicitacaoRetirada solicitacao,
            Long usuarioId,
            Long responsavelId,
            StatusRetirada statusAnterior,
            StatusRetirada statusNovo,
            String acao,
            String atorTipo,
            String motivoMovimentacao,
            String observacao,
            String justificativa,
            String motivoAuditoria) {

        SolicitacaoRetirada persistida = solicitacaoRetiradaRepository.saveAndFlush(solicitacao);

        MovimentacaoRetirada movimentacao = new MovimentacaoRetirada();
        movimentacao.setEscolaId(solicitacao.getEscolaId());
        movimentacao.setSolicitacaoRetiradaId(solicitacao.getId());
        movimentacao.setStatusAnterior(statusAnterior);
        movimentacao.setStatusNovo(statusNovo);
        movimentacao.setAtorTipo(atorTipo);
        movimentacao.setUsuarioId(usuarioId);
        movimentacao.setResponsavelId(responsavelId);
        movimentacao.setMotivo(normalizarTexto(motivoMovimentacao, acao));
        movimentacao.setJustificativa(justificativa);
        movimentacao.setDataOcorrencia(LocalDateTime.now());
        movimentacao.setObservacoes(observacao);
        movimentacaoRetiradaRepository.saveAndFlush(movimentacao);

        AuditoriaRegistro auditoria = AuditoriaRegistro.novoRegistro(
                solicitacao.getEscolaId(),
                "SolicitacaoRetirada",
                solicitacao.getId(),
                acao,
                usuarioId,
                responsavelId,
                normalizarTexto(motivoAuditoria, acao),
                String.format("{\"statusAnterior\":\"%s\",\"statusNovo\":\"%s\",\"atorTipo\":\"%s\"}",
                        statusAnterior,
                        statusNovo,
                        atorTipo),
                "local"
        );
        auditoriaRegistroRepository.saveAndFlush(auditoria);

        return persistida;
    }

    private String normalizarTexto(String valor, String fallback) {
        if (valor == null || valor.isBlank()) {
            return fallback;
        }
        return valor;
    }
}
