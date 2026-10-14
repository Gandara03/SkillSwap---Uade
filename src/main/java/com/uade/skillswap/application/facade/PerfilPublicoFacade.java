package com.uade.skillswap.application.facade;

import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.dto.CalificacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PerfilPublicoDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.port.in.CalificacionUseCase;
import com.uade.skillswap.application.port.in.PerfilPublicoUseCase;
import com.uade.skillswap.application.port.in.PerfilUseCase;
import com.uade.skillswap.application.port.in.PublicacionUseCase;
import com.uade.skillswap.domain.query.PaginaRequest;

import java.util.List;

/**
 * Facade: arma la vista resumida de un perfil coordinando tres casos de uso (perfil, calificaciones y
 * publicaciones). Quien la usa pide una sola cosa y no conoce a esos subsistemas ni el orden en que se llaman.
 */
public class PerfilPublicoFacade implements PerfilPublicoUseCase {

    private final PerfilUseCase perfiles;
    private final CalificacionUseCase calificaciones;
    private final PublicacionUseCase publicaciones;
    private final SkillSwapConfig config;

    public PerfilPublicoFacade(PerfilUseCase perfiles, CalificacionUseCase calificaciones,
                               PublicacionUseCase publicaciones, SkillSwapConfig config) {
        this.perfiles = perfiles;
        this.calificaciones = calificaciones;
        this.publicaciones = publicaciones;
        this.config = config;
    }

    @Override
    public PerfilPublicoDto verPerfilPublico(Long perfilId) {
        PerfilDto perfil = perfiles.obtener(perfilId);

        int nCalificaciones = config.ultimasCalificacionesPerfilPublico();
        List<CalificacionDto> ultimas = calificaciones
                .listarRecibidas(perfilId, PaginaRequest.de(0, tamano(nCalificaciones)))
                .contenido().stream().limit(Math.max(nCalificaciones, 0)).toList();

        List<PublicacionResumenDto> activas =
                publicaciones.listarPublicadasDe(perfilId, config.ultimasPublicacionesPerfilPublico());

        return new PerfilPublicoDto(perfil.id(), perfil.nombreVisible(), perfil.biografia(), perfil.habilidades(),
                perfil.reputacion(), perfil.cantidadCalificaciones(), ultimas, activas);
    }

    private static int tamano(int solicitado) {
        return Math.min(Math.max(solicitado, 1), PaginaRequest.TAMANO_MAXIMO);
    }
}
