package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.EditarPublicacionCommand;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.port.in.PublicacionUseCase;
import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

import java.time.LocalDateTime;

/** Adaptador delgado: traduce la UI a PublicacionUseCase. Sin reglas de negocio. */
public class PublicacionController implements IPublicacionController {

    private final PublicacionUseCase publicaciones;
    private final SesionUseCase sesion;

    public PublicacionController(PublicacionUseCase publicaciones, SesionUseCase sesion) {
        this.publicaciones = publicaciones;
        this.sesion = sesion;
    }

    @Override
    public Resultado<PublicacionDto> crearBorrador(CrearPublicacionCommand comando) {
        return Ejecucion.conActor(sesion, actor -> publicaciones.crearBorrador(actor, comando));
    }

    @Override
    public Resultado<PublicacionDto> editar(Long publicacionId, EditarPublicacionCommand comando) {
        return Ejecucion.conActor(sesion, actor -> publicaciones.editar(actor, publicacionId, comando));
    }

    @Override
    public Resultado<PublicacionDto> agregarFranja(Long publicacionId, LocalDateTime inicio, LocalDateTime fin) {
        return Ejecucion.conActor(sesion, actor -> publicaciones.agregarFranja(actor, publicacionId, inicio, fin));
    }

    @Override
    public Resultado<PublicacionDto> cambiarEstado(Long publicacionId, AccionPublicacion accion, String motivo) {
        if (accion == null) {
            return Ejecucion.conActor(sesion, actor -> {
                throw new com.uade.skillswap.application.exception.ValidacionException("La accion es obligatoria");
            });
        }
        return switch (accion) {
            case PUBLICAR -> publicar(publicacionId);
            case PAUSAR -> pausar(publicacionId);
            case REANUDAR -> reanudar(publicacionId);
            case CERRAR -> cerrar(publicacionId);
            case CANCELAR -> cancelar(publicacionId, motivo);
        };
    }

    @Override
    public Resultado<PublicacionDto> publicar(Long publicacionId) {
        return Ejecucion.conActor(sesion, actor -> publicaciones.publicar(actor, publicacionId));
    }

    @Override
    public Resultado<PublicacionDto> pausar(Long publicacionId) {
        return Ejecucion.conActor(sesion, actor -> publicaciones.pausar(actor, publicacionId));
    }

    @Override
    public Resultado<PublicacionDto> reanudar(Long publicacionId) {
        return Ejecucion.conActor(sesion, actor -> publicaciones.reanudar(actor, publicacionId));
    }

    @Override
    public Resultado<PublicacionDto> cerrar(Long publicacionId) {
        return Ejecucion.conActor(sesion, actor -> publicaciones.cerrar(actor, publicacionId));
    }

    @Override
    public Resultado<PublicacionDto> cancelar(Long publicacionId, String motivo) {
        return Ejecucion.conActor(sesion, actor -> publicaciones.cancelar(actor, publicacionId, motivo));
    }

    @Override
    public Resultado<PublicacionDto> obtener(Long publicacionId) {
        return Ejecucion.conActor(sesion, actor -> publicaciones.obtener(actor, publicacionId));
    }

    @Override
    public Resultado<Pagina<PublicacionResumenDto>> misPublicaciones(PaginaRequest pagina) {
        return Ejecucion.conActor(sesion, actor -> publicaciones.listarPropias(actor, pagina));
    }

    @Override
    public Resultado<PublicacionDto> agregarPromocion(CrearPromocionCommand comando) {
        return Ejecucion.conActor(sesion, actor -> publicaciones.agregarPromocion(actor, comando));
    }
}
