package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.dto.CalificacionDto;
import com.uade.skillswap.application.port.in.CalificacionUseCase;
import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Adaptador delgado: traduce la UI a CalificacionUseCase. */
public class CalificacionController implements ICalificacionController {

    private final CalificacionUseCase calificaciones;
    private final SesionUseCase sesion;

    public CalificacionController(CalificacionUseCase calificaciones, SesionUseCase sesion) {
        this.calificaciones = calificaciones;
        this.sesion = sesion;
    }

    @Override
    public Resultado<CalificacionDto> calificar(CalificarCommand comando) {
        return Ejecucion.conActor(sesion, actor -> calificaciones.calificar(actor, comando));
    }

    @Override
    public Resultado<Pagina<CalificacionDto>> recibidasDe(Long perfilId, PaginaRequest pagina) {
        return Ejecucion.sinActor(() -> calificaciones.listarRecibidas(perfilId, pagina));
    }

    @Override
    public Resultado<Pagina<CalificacionDto>> misRecibidas(PaginaRequest pagina) {
        return Ejecucion.conActor(sesion, actor -> calificaciones.listarRecibidas(actor, pagina));
    }
}
