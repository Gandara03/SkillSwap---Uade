package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.EditarPublicacionCommand;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

import java.time.LocalDateTime;

/** Contrato de la UI para publicaciones (mentorias, servicios y desafios). */
public interface IPublicacionController {

    Resultado<PublicacionDto> crearBorrador(CrearPublicacionCommand comando);

    Resultado<PublicacionDto> editar(Long publicacionId, EditarPublicacionCommand comando);

    Resultado<PublicacionDto> agregarFranja(Long publicacionId, LocalDateTime inicio, LocalDateTime fin);

    /** Transiciones de estado en una sola operacion. El motivo solo aplica a CANCELAR. */
    Resultado<PublicacionDto> cambiarEstado(Long publicacionId, AccionPublicacion accion, String motivo);

    Resultado<PublicacionDto> publicar(Long publicacionId);

    Resultado<PublicacionDto> pausar(Long publicacionId);

    Resultado<PublicacionDto> reanudar(Long publicacionId);

    Resultado<PublicacionDto> cerrar(Long publicacionId);

    Resultado<PublicacionDto> cancelar(Long publicacionId, String motivo);

    Resultado<PublicacionDto> obtener(Long publicacionId);

    Resultado<Pagina<PublicacionResumenDto>> misPublicaciones(PaginaRequest pagina);

    Resultado<PublicacionDto> agregarPromocion(CrearPromocionCommand comando);
}
