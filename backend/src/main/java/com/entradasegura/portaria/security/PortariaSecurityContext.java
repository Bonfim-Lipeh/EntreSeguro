package com.entradasegura.portaria.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class PortariaSecurityContext {

    private PortariaSecurityContext() {
    }

    public static PortariaPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Autenticação de portaria obrigatória para consultar dados da escola.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof PortariaPrincipal portariaPrincipal) {
            return portariaPrincipal;
        }

        throw new IllegalStateException(
                "Autenticação pendente de integração: o principal autenticado não expõe usuarioId e escolaId da portaria."
        );
    }

    public static Long currentEscolaId() {
        return currentPrincipal().getEscolaId();
    }

    public static Long currentUsuarioId() {
        return currentPrincipal().getUsuarioId();
    }

    public static String currentUsuarioNome() {
        return currentPrincipal().getNome();
    }
}
