package com.uade.skillswap.application.port.in;

import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.dto.CalificacionDto;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

public interface CalificacionUseCase {

    /** Registra la calificacion y recalcula la reputacion del destinatario en la misma transaccion. */
    CalificacionDto calificar(Long actorId, CalificarCommand comando);

    Pagina<CalificacionDto> listarRecibidas(Long perfilId, PaginaRequest pagina);
}
