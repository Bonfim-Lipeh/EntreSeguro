package com.entradasegura.people.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "vinculo_aluno_responsavel",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vinculo_principal", columnNames = {"escola_id", "aluno_id", "principal"})
        },
        indexes = {
                @Index(name = "idx_vinculo_escola", columnList = "escola_id"),
                @Index(name = "idx_vinculo_aluno", columnList = "aluno_id"),
                @Index(name = "idx_vinculo_responsavel", columnList = "responsavel_id")
        }
)
public class VinculoAlunoResponsavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "escola_id", nullable = false)
    private Long escolaId;

    @Column(name = "aluno_id", nullable = false)
    private Long alunoId;

    @Column(name = "responsavel_id", nullable = false)
    private Long responsavelId;

    @Column(name = "tipo_relacao", nullable = false, length = 30)
    private String tipoRelacao;

    @Column(nullable = false)
    private Boolean principal;

    @Column(nullable = false)
    private Boolean ativo;

    @Column(name = "validado_em")
    private LocalDateTime validadoEm;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public VinculoAlunoResponsavel() {
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

    public String getTipoRelacao() {
        return tipoRelacao;
    }

    public void setTipoRelacao(String tipoRelacao) {
        this.tipoRelacao = tipoRelacao;
    }

    public Boolean getPrincipal() {
        return principal;
    }

    public void setPrincipal(Boolean principal) {
        this.principal = principal;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getValidadoEm() {
        return validadoEm;
    }

    public void setValidadoEm(LocalDateTime validadoEm) {
        this.validadoEm = validadoEm;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
