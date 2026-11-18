package com.uade.skillswap.application.facade;

import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.dto.CalificacionDto;
import com.uade.skillswap.application.dto.ContratacionCompletadaDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PerfilPublicoDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.port.in.CalificacionUseCase;
import com.uade.skillswap.application.port.in.ContratacionUseCase;
import com.uade.skillswap.application.port.in.PerfilPublicoUseCase;
import com.uade.skillswap.application.port.in.PerfilUseCase;
import com.uade.skillswap.application.port.in.PublicacionUseCase;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

import java.util.List;

/**
 * Facade: arma la vista resumida de un perfil coordinando cuatro casos de uso (perfil, calificaciones,
 * publicaciones y contrataciones). Quien la usa pide una sola cosa y no conoce a esos subsistemas ni el orden en
 * que se llaman.
 */
public class PerfilPublicoFacade implements PerfilPublicoUseCase {

    private final PerfilUseCase perfiles;
    private final CalificacionUseCase calificaciones;
    private final PublicacionUseCase publicaciones;
    private final ContratacionUseCase contrataciones;
    private final SkillSwapConfig config;

    public PerfilPublicoFacade(PerfilUseCase perfiles, CalificacionUseCase calificaciones,
                               PublicacionUseCase publicaciones, ContratacionUseCase contrataciones,
                               SkillSwapConfig config) {
        this.perfiles = perfiles;
        this.calificaciones = calificaciones;
        this.publicaciones = publicaciones;
        this.contrataciones = contrataciones;
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

        // el total sale del count de la misma pagina: no hace falta otra consulta
        int nCompletadas = config.ultimasCompletadasPerfilPublico();
        Pagina<ContratacionCompletadaDto> completadas = contrataciones
                .listarCompletadasComoProveedor(perfilId, PaginaRequest.de(0, tamano(nCompletadas)));
        List<ContratacionCompletadaDto> ultimasCompletadas =
                completadas.contenido().stream().limit(Math.max(nCompletadas, 0)).toList();

        return new PerfilPublicoDto(perfil.id(), perfil.nombreVisible(), perfil.biografia(), perfil.habilidades(),
                perfil.reputacion(), perfil.cantidadCalificaciones(), ultimas, activas,
                Math.toIntExact(completadas.totalElementos()), ultimasCompletadas);
    }

    private static int tamano(int solicitado) {
        return Math.min(Math.max(solicitado, 1), PaginaRequest.TAMANO_MAXIMO);
    }
}
