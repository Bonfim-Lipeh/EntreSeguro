package com.entradasegura.people.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "aluno",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_aluno_escola_matricula", columnNames = {"escola_id", "matricula"}),
                @UniqueConstraint(name = "uk_aluno_escola_chave", columnNames = {"escola_id", "id"})
        },
        indexes = {
                @Index(name = "idx_aluno_escola", columnList = "escola_id"),
                @Index(name = "idx_aluno_turma", columnList = "turma_id"),
                @Index(name = "idx_aluno_status", columnList = "status")
        }
)
public class Aluno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "escola_id", nullable = false)
    private Long escolaId;

    @Column(name = "turma_id", nullable = false)
    private Long turmaId;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "data_nascimento", nullable = false)
    private LocalDate dataNascimento;

    @Column(nullable = false, length = 50)
    private String matricula;

    @Column(name = "documento_criptografado", nullable = false, length = 512)
    private String documentoCriptografado;

    @Column(name = "documento_hmac", nullable = false, length = 128)
    private String documentoHmac;

    @Column(nullable = false, length = 30)
    private String status;

    @Version
    private Long versao;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @UpdateTimestamp
    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    public Aluno() {
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

    public Long getTurmaId() {
        return turmaId;
    }

    public void setTurmaId(Long turmaId) {
        this.turmaId = turmaId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public String getMatricula() {
        return matricula;
    }

    public void setMatricula(String matricula) {
        this.matricula = matricula;
    }

    public String getDocumentoCriptografado() {
        return documentoCriptografado;
    }

    public void setDocumentoCriptografado(String documentoCriptografado) {
        this.documentoCriptografado = documentoCriptografado;
    }

    public String getDocumentoHmac() {
        return documentoHmac;
    }

    public void setDocumentoHmac(String documentoHmac) {
        this.documentoHmac = documentoHmac;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
