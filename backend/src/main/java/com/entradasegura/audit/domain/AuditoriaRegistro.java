package com.entradasegura.audit.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "auditoria_registro",
        indexes = {
                @Index(name = "idx_auditoria_escola", columnList = "escola_id"),
                @Index(name = "idx_auditoria_entidade", columnList = "entidade"),
                @Index(name = "idx_auditoria_usuario", columnList = "usuario_id"),
                @Index(name = "idx_auditoria_responsavel", columnList = "responsavel_id")
        }
)
public class AuditoriaRegistro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "escola_id", nullable = false)
    private Long escolaId;

    @Column(nullable = false, length = 100)
    private String entidade;

    @Column(name = "entidade_id", nullable = false)
    private Long entidadeId;

    @Column(nullable = false, length = 30)
    private String acao;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "responsavel_id")
    private Long responsavelId;

    @Column(nullable = false, length = 100)
    private String motivo;

    @Column(name = "detalhes_json", length = 2000)
    private String detalhesJson;

    @Column(name = "ip_origem", length = 45)
    private String ipOrigem;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    protected AuditoriaRegistro() {
    }

    public static AuditoriaRegistro novoRegistro(
            Long escolaId,
            String entidade,
            Long entidadeId,
            String acao,
            Long usuarioId,
            Long responsavelId,
            String motivo,
            String detalhesJson,
            String ipOrigem
    ) {
        AuditoriaRegistro auditoria = new AuditoriaRegistro();
        auditoria.escolaId = escolaId;
        auditoria.entidade = entidade;
        auditoria.entidadeId = entidadeId;
        auditoria.acao = acao;
        auditoria.usuarioId = usuarioId;
        auditoria.responsavelId = responsavelId;
        auditoria.motivo = motivo;
        auditoria.detalhesJson = detalhesJson;
        auditoria.ipOrigem = ipOrigem;
        auditoria.criadoEm = LocalDateTime.now();
        return auditoria;
    }

    public Long getId() {
        return id;
    }

    public Long getEscolaId() {
        return escolaId;
    }

    public String getEntidade() {
        return entidade;
    }

    public Long getEntidadeId() {
        return entidadeId;
    }

    public String getAcao() {
        return acao;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getResponsavelId() {
        return responsavelId;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getDetalhesJson() {
        return detalhesJson;
    }

    public String getIpOrigem() {
        return ipOrigem;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }
}
