package com.entradasegura.people.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "responsavel",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_responsavel_escola_email", columnNames = {"escola_id", "email"}),
                @UniqueConstraint(name = "uk_responsavel_escola_chave", columnNames = {"escola_id", "id"})
        },
        indexes = {
                @Index(name = "idx_responsavel_escola", columnList = "escola_id"),
                @Index(name = "idx_responsavel_usuario", columnList = "usuario_id"),
                @Index(name = "idx_responsavel_status", columnList = "status")
        }
)
public class Responsavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "escola_id", nullable = false)
    private Long escolaId;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(name = "documento_criptografado", nullable = false, length = 512)
    private String documentoCriptografado;

    @Column(name = "documento_hmac", nullable = false, length = 128)
    private String documentoHmac;

    @Column(length = 30)
    private String telefone;

    @Column(length = 200)
    private String email;

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

    public Responsavel() {
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

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
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

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
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
