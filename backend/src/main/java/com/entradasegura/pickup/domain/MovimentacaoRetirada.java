package com.entradasegura.pickup.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "movimentacao_retirada",
        indexes = {
                @Index(name = "idx_movimentacao_escola", columnList = "escola_id"),
                @Index(name = "idx_movimentacao_solicitacao", columnList = "solicitacao_retirada_id"),
                @Index(name = "idx_movimentacao_status", columnList = "status_novo"),
                @Index(name = "idx_movimentacao_usuario", columnList = "usuario_id")
        }
)
public class MovimentacaoRetirada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "escola_id", nullable = false)
    private Long escolaId;

    @Column(name = "solicitacao_retirada_id", nullable = false)
    private Long solicitacaoRetiradaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", nullable = false, length = 30)
    private StatusRetirada statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, length = 30)
    private StatusRetirada statusNovo;

    @Column(name = "ator_tipo", nullable = false, length = 30)
    private String atorTipo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "responsavel_id")
    private Long responsavelId;

    @Column(nullable = false, length = 100)
    private String motivo;

    @Column(length = 500)
    private String justificativa;

    @Column(name = "data_ocorrencia", nullable = false)
    private LocalDateTime dataOcorrencia;

    @Column(length = 500)
    private String observacoes;

    @Version
    private Long versao;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public MovimentacaoRetirada() {
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

    public Long getSolicitacaoRetiradaId() {
        return solicitacaoRetiradaId;
    }

    public void setSolicitacaoRetiradaId(Long solicitacaoRetiradaId) {
        this.solicitacaoRetiradaId = solicitacaoRetiradaId;
    }

    public StatusRetirada getStatusAnterior() {
        return statusAnterior;
    }

    public void setStatusAnterior(StatusRetirada statusAnterior) {
        this.statusAnterior = statusAnterior;
    }

    public StatusRetirada getStatusNovo() {
        return statusNovo;
    }

    public void setStatusNovo(StatusRetirada statusNovo) {
        this.statusNovo = statusNovo;
    }

    public String getAtorTipo() {
        return atorTipo;
    }

    public void setAtorTipo(String atorTipo) {
        this.atorTipo = atorTipo;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }

    public void setResponsavelId(Long responsavelId) {
        this.responsavelId = responsavelId;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public LocalDateTime getDataOcorrencia() {
        return dataOcorrencia;
    }

    public void setDataOcorrencia(LocalDateTime dataOcorrencia) {
        this.dataOcorrencia = dataOcorrencia;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
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
}
