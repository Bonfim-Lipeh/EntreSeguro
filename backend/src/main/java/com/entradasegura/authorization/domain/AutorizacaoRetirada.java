package com.entradasegura.authorization.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "autorizacao_retirada",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_autorizacao_escola_aluno", columnNames = {"escola_id", "aluno_id"})
        },
        indexes = {
                @Index(name = "idx_autorizacao_escola", columnList = "escola_id"),
                @Index(name = "idx_autorizacao_aluno", columnList = "aluno_id"),
                @Index(name = "idx_autorizacao_responsavel", columnList = "responsavel_id"),
                @Index(name = "idx_autorizacao_status", columnList = "status")
        }
)
public class AutorizacaoRetirada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "escola_id", nullable = false)
    private Long escolaId;

    @Column(name = "aluno_id", nullable = false)
    private Long alunoId;

    @Column(name = "responsavel_id", nullable = false)
    private Long responsavelId;

    @Column(nullable = false, length = 30)
    private String tipo;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDateTime dataFim;

    @Column(length = 500)
    private String observacoes;

    @Version
    private Long versao;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public AutorizacaoRetirada() {
    }

    /**
     * Regra de vigência: status ATIVA e o instante dentro do período [dataInicio, dataFim].
     * Autorizações REVOGADAS (ou qualquer outro status) e fora do período não são vigentes.
     */
    public boolean estaVigenteEm(LocalDateTime instante) {
        return "ATIVA".equalsIgnoreCase(status)
                && dataInicio != null && dataFim != null
                && !instante.isBefore(dataInicio)
                && !instante.isAfter(dataFim);
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDateTime dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDateTime getDataFim() {
        return dataFim;
    }

    public void setDataFim(LocalDateTime dataFim) {
        this.dataFim = dataFim;
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

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
