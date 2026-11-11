package com.entradasegura.pickup.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "solicitacao_retirada",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_solicitacao_ativa_por_aluno_escola", columnNames = {"aluno_id", "escola_id", "ativa"})
        },
        indexes = {
                @Index(name = "idx_solicitacao_aluno", columnList = "aluno_id"),
                @Index(name = "idx_solicitacao_escola", columnList = "escola_id"),
                @Index(name = "idx_solicitacao_status", columnList = "status"),
                @Index(name = "idx_solicitacao_ativa", columnList = "ativa")
        }
)
public class SolicitacaoRetirada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "escola_id", nullable = false)
    private Long escolaId;

    @Column(name = "aluno_id", nullable = false)
    private Long alunoId;

    @Column(name = "responsavel_id", nullable = false)
    private Long responsavelId;

    @Column(name = "funcionario_id")
    private Long funcionarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusRetirada status;

    @Column(nullable = false)
    private boolean ativa = true;

    @Column(name = "motivo")
    private String motivo;

    @Column(name = "observacao")
    private String observacao;

    @Column(name = "justificativa_rejeicao")
    private String justificativaRejeicao;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_excecao")
    private MotivoExcecaoRetirada motivoExcecao;

    @Column(name = "qr_code_hash", length = 255)
    private String qrCodeHash;

    @Column(name = "qr_code_validado_em")
    private LocalDateTime qrCodeValidadoEm;

    @Column(name = "data_solicitacao", nullable = false)
    private LocalDateTime dataSolicitacao;

    @Column(name = "data_atendimento")
    private LocalDateTime dataAtendimento;

    @Column(name = "data_conclusao")
    private LocalDateTime dataConclusao;

    @Version
    private Long versao;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public SolicitacaoRetirada() {
    }

    public static SolicitacaoRetirada novaSolicitacao(Long escolaId, Long alunoId, Long responsavelId, String motivo, LocalDateTime dataSolicitacao) {
        SolicitacaoRetirada solicitacao = new SolicitacaoRetirada();
        solicitacao.escolaId = escolaId;
        solicitacao.alunoId = alunoId;
        solicitacao.responsavelId = responsavelId;
        solicitacao.status = StatusRetirada.AGUARDANDO;
        solicitacao.ativa = true;
        solicitacao.motivo = motivo;
        solicitacao.dataSolicitacao = dataSolicitacao;
        return solicitacao;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEscolaId() {
        return escolaId;
    }

    public void setEscolaId(Long escolaId) {
        this.escolaId = escolaId;
    }

    public Long getAlunoId() {
        return alunoId;
    }

    public void setAlunoId(Long alunoId) {
        this.alunoId = alunoId;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(Long responsavelId) {
        this.responsavelId = responsavelId;
    }

    public Long getFuncionarioId() {
        return funcionarioId;
    }

    public void setFuncionarioId(Long funcionarioId) {
        this.funcionarioId = funcionarioId;
    }

    public StatusRetirada getStatus() {
        return status;
    }

    private void setStatusInternal(StatusRetirada status) {
        this.status = status;
    }

    private void setAtivaInternal(boolean ativa) {
        this.ativa = ativa;
    }

    public boolean isAtiva() {
        return ativa;
    }

    public boolean isTerminal() {
        return status == StatusRetirada.CONCLUIDA || status == StatusRetirada.REJEITADA || status == StatusRetirada.CANCELADA;
    }

    public void iniciarAtendimento(Long funcionarioId, String observacao) {
        validarAtor(funcionarioId, "FUNCIONARIO");
        if (this.status != StatusRetirada.AGUARDANDO) {
            throw new IllegalStateException("Só é permitido iniciar atendimento quando a solicitação está em AGUARDANDO.");
        }
        this.funcionarioId = funcionarioId;
        this.observacao = observacao;
        this.dataAtendimento = LocalDateTime.now();
        setStatusInternal(StatusRetirada.EM_ATENDIMENTO);
        setAtivaInternal(true);
    }

    public void confirmarIdentificacao(Long funcionarioId, String observacao) {
        validarAtor(funcionarioId, "FUNCIONARIO");
        if (this.status != StatusRetirada.EM_ATENDIMENTO) {
            throw new IllegalStateException("Só é permitido confirmar identificação quando a solicitação está em EM_ATENDIMENTO.");
        }
        this.funcionarioId = funcionarioId;
        this.observacao = observacao;
        this.dataAtendimento = this.dataAtendimento == null ? LocalDateTime.now() : this.dataAtendimento;
        setStatusInternal(StatusRetirada.IDENTIFICADO);
        setAtivaInternal(true);
    }

    public void rejeitar(Long funcionarioId, String justificativa, MotivoExcecaoRetirada motivoExcecao, String observacao) {
        validarAtor(funcionarioId, "FUNCIONARIO");
        if (justificativa == null || justificativa.isBlank()) {
            throw new IllegalArgumentException("Justificativa obrigatória para rejeição.");
        }

        boolean permitidoEmAtendimento = this.status == StatusRetirada.EM_ATENDIMENTO;
        boolean permitidoIdentificado = this.status == StatusRetirada.IDENTIFICADO
                && motivoExcecao == MotivoExcecaoRetirada.AUTORIZACAO_EXPIRADA;

        if (!permitidoEmAtendimento && !permitidoIdentificado) {
            throw new IllegalStateException("Rejeição não permitida para o estado atual da solicitação.");
        }

        this.funcionarioId = funcionarioId;
        this.justificativaRejeicao = justificativa;
        this.motivoExcecao = motivoExcecao;
        this.observacao = observacao;
        setStatusInternal(StatusRetirada.REJEITADA);
        setAtivaInternal(false);
    }

    public void confirmarRetirada(Long funcionarioId, String observacao) {
        validarAtor(funcionarioId, "FUNCIONARIO");
        if (this.status != StatusRetirada.IDENTIFICADO) {
            throw new IllegalStateException("Conclusão só é permitida a partir de IDENTIFICADO.");
        }
        this.funcionarioId = funcionarioId;
        this.observacao = observacao;
        this.dataConclusao = LocalDateTime.now();
        setStatusInternal(StatusRetirada.CONCLUIDA);
        setAtivaInternal(false);
    }

    public void cancelarPorResponsavel(Long responsavelId, String motivo, String observacao) {
        validarAtor(responsavelId, "RESPONSAVEL");
        if (this.status != StatusRetirada.AGUARDANDO) {
            throw new IllegalStateException("Cancelamento só é permitido quando a solicitação está em AGUARDANDO.");
        }
        if (this.responsavelId == null || !this.responsavelId.equals(responsavelId)) {
            throw new IllegalArgumentException("O responsável informado não corresponde à solicitação.");
        }
        this.motivo = motivo;
        this.observacao = observacao;
        setStatusInternal(StatusRetirada.CANCELADA);
        setAtivaInternal(false);
    }

    public void garantirEstadoTerminalNaoAlteravel() {
        if (isTerminal()) {
            throw new IllegalStateException("Solicitações em estado final não podem ser alteradas.");
        }
    }

    private void validarAtor(Long atorId, String tipoAtor) {
        if (atorId == null) {
            throw new IllegalArgumentException("Identidade do ator obrigatória.");
        }
        if (tipoAtor == null || tipoAtor.isBlank()) {
            throw new IllegalArgumentException("Tipo do ator obrigatório.");
        }
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public String getJustificativaRejeicao() {
        return justificativaRejeicao;
    }

    public void setJustificativaRejeicao(String justificativaRejeicao) {
        this.justificativaRejeicao = justificativaRejeicao;
    }

    public MotivoExcecaoRetirada getMotivoExcecao() {
        return motivoExcecao;
    }

    public void setMotivoExcecao(MotivoExcecaoRetirada motivoExcecao) {
        this.motivoExcecao = motivoExcecao;
    }

    public String getQrCodeHash() {
        return qrCodeHash;
    }

    public void setQrCodeHash(String qrCodeHash) {
        this.qrCodeHash = qrCodeHash;
    }

    public LocalDateTime getQrCodeValidadoEm() {
        return qrCodeValidadoEm;
    }

    public void setQrCodeValidadoEm(LocalDateTime qrCodeValidadoEm) {
        this.qrCodeValidadoEm = qrCodeValidadoEm;
    }

    public LocalDateTime getDataSolicitacao() {
        return dataSolicitacao;
    }

    public void setDataSolicitacao(LocalDateTime dataSolicitacao) {
        this.dataSolicitacao = dataSolicitacao;
    }

    public LocalDateTime getDataAtendimento() {
        return dataAtendimento;
    }

    public void setDataAtendimento(LocalDateTime dataAtendimento) {
        this.dataAtendimento = dataAtendimento;
    }

    public LocalDateTime getDataConclusao() {
        return dataConclusao;
    }

    public void setDataConclusao(LocalDateTime dataConclusao) {
        this.dataConclusao = dataConclusao;
    }

    public Long getVersao() {
        return versao;
    }

    public void setVersao(Long versao) {
        this.versao = versao;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
