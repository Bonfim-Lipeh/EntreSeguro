package com.entradasegura.portaria.security;

import com.entradasegura.identity.domain.PerfilUsuario;
import com.entradasegura.identity.domain.Usuario;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class PortariaPrincipal implements UserDetails {

    private final Long usuarioId;
    private final Long escolaId;
    private final String nome;
    private final String email;
    private final PerfilUsuario perfil;
    private final String senhaHash;
    private final boolean enabled;
    private final Collection<? extends GrantedAuthority> authorities;

    public PortariaPrincipal(Long usuarioId, Long escolaId, String nome, String email, PerfilUsuario perfil, String senhaHash, boolean enabled) {
        this.usuarioId = usuarioId;
        this.escolaId = escolaId;
        this.nome = nome;
        this.email = email;
        this.perfil = perfil;
        this.senhaHash = senhaHash;
        this.enabled = enabled;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + perfil.name()));
    }

    public static PortariaPrincipal from(Usuario usuario) {
        return new PortariaPrincipal(
                usuario.getId(),
                usuario.getEscolaId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPerfil(),
                usuario.getSenhaHash(),
                "ATIVO".equalsIgnoreCase(usuario.getStatus())
        );
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getEscolaId() {
        return escolaId;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public PerfilUsuario getPerfil() {
        return perfil;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
