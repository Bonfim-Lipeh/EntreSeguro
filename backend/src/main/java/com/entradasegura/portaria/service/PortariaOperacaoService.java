package com.entradasegura.portaria.service;

import com.entradasegura.authorization.domain.AutorizacaoRetirada;
import com.entradasegura.pickup.domain.MotivoExcecaoRetirada;
import com.entradasegura.pickup.domain.SolicitacaoRetirada;
import com.entradasegura.pickup.domain.StatusRetirada;
import com.entradasegura.pickup.repository.SolicitacaoRetiradaRepository;
import com.entradasegura.pickup.service.SolicitacaoRetiradaTransitionService;
import com.entradasegura.portaria.api.dto.RetiradaDTO;
import com.entradasegura.portaria.security.PortariaPrincipal;
import com.entradasegura.portaria.security.PortariaSecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Operações do funcionário da portaria sobre uma solicitação de retirada.
 *
 * <p>Orquestra o caso de uso: isolamento por escola, concorrência otimista (versão),
 * revalidação da autorização do responsável e tradução de erros. As transições de estado
 * são decididas pela entidade {@link SolicitacaoRetirada}; status, movimentação e auditoria
 * são gravados pelo {@link SolicitacaoRetiradaTransitionService} na MESMA transação.
 * Funcionário e escola vêm sempre do JWT, nunca do corpo da requisição.</p>
 */
@Service
public class PortariaOperacaoService {

    static final int JUSTIFICATIVA_MIN = 10;
    static final int JUSTIFICATIVA_MAX = 500;

    private final SolicitacaoRetiradaRepository solicitacaoRetiradaRepository;
    private final SolicitacaoRetiradaTransitionService transicoes;
    private final PortariaService portariaService;

    public PortariaOperacaoService(
            SolicitacaoRetiradaRepository solicitacaoRetiradaRepository,
            SolicitacaoRetiradaTransitionService transicoes,
            PortariaService portariaService) {
        this.solicitacaoRetiradaRepository = solicitacaoRetiradaRepository;
        this.transicoes = transicoes;
        this.portariaService = portariaService;
    }

    @Transactional
    public RetiradaDTO iniciarAtendimento(Long id, Long versao) {
        PortariaPrincipal funcionario = PortariaSecurityContext.currentPrincipal();
        SolicitacaoRetirada solicitacao = carregar(id, funcionario, versao);
        return transicionar(() -> transicoes.iniciarAtendimento(
                solicitacao.getId(), funcionario.getUsuarioId(), null, "INICIAR_ATENDIMENTO"));
    }

    /**
     * Identificação manual: o funcionário confere o documento com foto da pessoa presente.
     * O backend confere a solicitação, a escola, o responsável ativo e a autorização vigente.
     * A validação por QR Code não está disponível e não é simulada.
     */
    @Transactional
    public RetiradaDTO confirmarIdentificacao(Long id, String metodo, Long versao) {
        if (metodo == null || metodo.isBlank()) {
            throw PortariaOperacaoException.validacao("METODO_OBRIGATORIO", "Informe o método de identificação.");
        }
        if ("QR_CODE".equalsIgnoreCase(metodo)) {
            throw PortariaOperacaoException.validacao("QR_NAO_VALIDADO",
                    "A validação de QR Code ainda não está disponível. Use a conferência de documento com foto.");
        }
        if (!"DOCUMENTO".equalsIgnoreCase(metodo)) {
            throw PortariaOperacaoException.validacao("METODO_INVALIDO", "Método de identificação inválido: " + metodo);
        }

        PortariaPrincipal funcionario = PortariaSecurityContext.currentPrincipal();
        SolicitacaoRetirada solicitacao = carregar(id, funcionario, versao);
        // Fora de EM_ATENDIMENTO o domínio recusa a transição (TRANSICAO_INVALIDA) antes de qualquer conferência.
        String resultado = null;
        if (solicitacao.getStatus() == StatusRetirada.EM_ATENDIMENTO) {
            AutorizacaoRetirada autorizacao = exigirAutorizacaoVigente(solicitacao);
            resultado = String.format(
                    "Documento com foto conferido pelo funcionário. Autorização #%d (%s) vigente até %s.",
                    autorizacao.getId(), autorizacao.getTipo(), autorizacao.getDataFim());
        }
        String observacao = resultado;

        return transicionar(() -> transicoes.confirmarIdentificacao(
                solicitacao.getId(), funcionario.getUsuarioId(), observacao,
                PortariaService.PREFIXO_IDENTIFICACAO + "DOCUMENTO"));
    }

    @Transactional
    public RetiradaDTO confirmarRetirada(Long id, Long versao) {
        PortariaPrincipal funcionario = PortariaSecurityContext.currentPrincipal();
        SolicitacaoRetirada solicitacao = carregar(id, funcionario, versao);
        // Revalidação no momento da saída (a autorização pode ter expirado após a identificação).
        if (solicitacao.getStatus() == StatusRetirada.IDENTIFICADO) {
            exigirAutorizacaoVigente(solicitacao);
        }
        return transicionar(() -> transicoes.confirmarRetirada(
                solicitacao.getId(), funcionario.getUsuarioId(), null, "RETIRADA_CONFIRMADA"));
    }

    @Transactional
    public RetiradaDTO rejeitar(Long id, String motivo, String justificativa, Long versao) {
        String texto = justificativa == null ? "" : justificativa.trim();
        if (texto.length() < JUSTIFICATIVA_MIN) {
            throw PortariaOperacaoException.validacao("JUSTIFICATIVA_OBRIGATORIA",
                    "Informe a justificativa da rejeição (mínimo de " + JUSTIFICATIVA_MIN + " caracteres).");
        }
        if (texto.length() > JUSTIFICATIVA_MAX) {
            throw PortariaOperacaoException.validacao("JUSTIFICATIVA_LONGA",
                    "A justificativa deve ter no máximo " + JUSTIFICATIVA_MAX + " caracteres.");
        }
        MotivoExcecaoRetirada motivoExcecao = converterMotivo(motivo);

        PortariaPrincipal funcionario = PortariaSecurityContext.currentPrincipal();
        SolicitacaoRetirada solicitacao = carregar(id, funcionario, versao);

        // Exceção do domínio (IDENTIFICADO -> REJEITADA por AUTORIZACAO_EXPIRADA): só quando a autorização de fato não está vigente.
        if (solicitacao.getStatus() == StatusRetirada.IDENTIFICADO
                && motivoExcecao == MotivoExcecaoRetirada.AUTORIZACAO_EXPIRADA
                && portariaService.autorizacaoVigente(solicitacao).isPresent()) {
            throw PortariaOperacaoException.transicaoInvalida(
                    "A autorização deste responsável está vigente; a solicitação identificada deve seguir para a confirmação da retirada.");
        }

        return transicionar(() -> transicoes.rejeitar(
                solicitacao.getId(), funcionario.getUsuarioId(), texto, motivoExcecao, null, motivoExcecao.name()));
    }

    private SolicitacaoRetirada carregar(Long id, PortariaPrincipal funcionario, Long versao) {
        if (versao == null) {
            throw PortariaOperacaoException.validacao("VERSAO_OBRIGATORIA", "Informe a versão da solicitação.");
        }
        // Solicitação de outra escola é tratada como inexistente.
        SolicitacaoRetirada solicitacao = solicitacaoRetiradaRepository.findByIdAndEscolaId(id, funcionario.getEscolaId())
                .orElseThrow(PortariaOperacaoException::naoEncontrada);
        long versaoAtual = solicitacao.getVersao() == null ? 0L : solicitacao.getVersao();
        if (!Objects.equals(versaoAtual, versao)) {
            throw PortariaOperacaoException.versaoDesatualizada();
        }
        return solicitacao;
    }

    private AutorizacaoRetirada exigirAutorizacaoVigente(SolicitacaoRetirada solicitacao) {
        return portariaService.autorizacaoVigente(solicitacao)
                .orElseThrow(PortariaOperacaoException::autorizacaoNaoVigente);
    }

    private MotivoExcecaoRetirada converterMotivo(String motivo) {
        try {
            return MotivoExcecaoRetirada.valueOf(motivo == null ? "" : motivo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw PortariaOperacaoException.validacao("MOTIVO_INVALIDO", "Motivo de rejeição inválido: " + motivo);
        }
    }

    /** Executa a transição do domínio e traduz suas recusas em erros de negócio da API. */
    private RetiradaDTO transicionar(Supplier<SolicitacaoRetirada> transicao) {
        SolicitacaoRetirada resultado;
        try {
            resultado = transicao.get();
        } catch (IllegalStateException e) {
            throw PortariaOperacaoException.transicaoInvalida(e.getMessage());
        } catch (IllegalArgumentException e) {
            throw PortariaOperacaoException.validacao("DADOS_INVALIDOS", e.getMessage());
        }
        return portariaService.mapearRetirada(resultado);
    }
}
