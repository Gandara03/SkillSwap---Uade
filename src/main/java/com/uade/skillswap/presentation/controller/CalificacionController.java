package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.dto.CalificacionDto;
import com.uade.skillswap.application.port.in.CalificacionUseCase;
import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Adaptador delgado: valida la entrada y la traduce a CalificacionUseCase. */
public class CalificacionController implements ICalificacionController {

    private final CalificacionUseCase calificaciones;
    private final SesionUseCase sesion;

    public CalificacionController(CalificacionUseCase calificaciones, SesionUseCase sesion) {
        this.calificaciones = calificaciones;
        this.sesion = sesion;
    }

    @Override
    public Resultado<CalificacionDto> calificar(CalificarCommand comando) {
        return Ejecucion.conActor(sesion, actor -> {
            validar(comando);
            return calificaciones.calificar(actor, comando);
        });
    }

    @Override
    public Resultado<Pagina<CalificacionDto>> recibidasDe(Long perfilId, PaginaRequest pagina) {
        return Ejecucion.sinActor(() -> {
            ValidadorEntrada.exigirId(perfilId, "el perfil");
            ValidadorEntrada.exigirPagina(pagina);
            return calificaciones.listarRecibidas(perfilId, pagina);
        });
    }

    @Override
    public Resultado<Pagina<CalificacionDto>> misRecibidas(PaginaRequest pagina) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirPagina(pagina);
            return calificaciones.listarRecibidas(actor, pagina);
        });
    }

    private static void validar(CalificarCommand c) {
        ValidadorEntrada.exigir(c, "los datos de la calificacion");
        ValidadorEntrada.exigirId(c.contratacionId(), "la contratacion");
        ValidadorEntrada.exigirEntre(c.puntaje(), ValidadorEntrada.PUNTAJE_MINIMO, ValidadorEntrada.PUNTAJE_MAXIMO,
                "el puntaje");
        ValidadorEntrada.exigirLargo(c.comentario(), ValidadorEntrada.LARGO_COMENTARIO, "el comentario");
    }
}
