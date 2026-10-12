package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.dto.CalificacionDto;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Contrato de la UI para calificaciones. */
public interface ICalificacionController {

    Resultado<CalificacionDto> calificar(CalificarCommand comando);

    /** Calificaciones recibidas por cualquier perfil (son publicas). */
    Resultado<Pagina<CalificacionDto>> recibidasDe(Long perfilId, PaginaRequest pagina);

    Resultado<Pagina<CalificacionDto>> misRecibidas(PaginaRequest pagina);
}
