package com.uade.skillswap.application.service;

import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.application.port.out.IdentidadPort;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.application.port.out.PerfilRepository;

/** Resuelve el actor actual: usuario autenticado (IdentidadPort) -> perfil activo. */
public class SesionService implements SesionUseCase {

    private final IdentidadPort identidad;
    private final PerfilRepository perfilRepository;

    public SesionService(IdentidadPort identidad, PerfilRepository perfilRepository) {
        this.identidad = identidad;
        this.perfilRepository = perfilRepository;
    }

    @Override
    public Long perfilIdActual() {
        IdentidadPort.UsuarioIdentidad usuario = identidad.usuarioActual()
                .orElseThrow(() -> new NoAutorizadoException("No hay una sesion iniciada"));
        if (!usuario.activo()) {
            throw new NoAutorizadoException("El usuario esta inactivo en el servicio de identidad");
        }
        Perfil perfil = perfilRepository.buscarPorUserId(usuario.userId())
                .orElseThrow(() -> new NoAutorizadoException("El usuario no tiene un perfil registrado"));
        if (!perfil.estaActivo()) {
            throw new NoAutorizadoException("El perfil esta dado de baja");
        }
        return perfil.getId();
    }
}
