package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.port.in.NotificacionUseCase;
import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Adaptador delgado: traduce la UI a NotificacionUseCase. */
public class NotificacionController implements INotificacionController {

    private final NotificacionUseCase notificaciones;
    private final SesionUseCase sesion;

    public NotificacionController(NotificacionUseCase notificaciones, SesionUseCase sesion) {
        this.notificaciones = notificaciones;
        this.sesion = sesion;
    }

    @Override
    public Resultado<Pagina<NotificacionDto>> listar(boolean soloNoLeidas, PaginaRequest pagina) {
        return Ejecucion.conActor(sesion, actor -> notificaciones.listar(actor, soloNoLeidas, pagina));
    }

    @Override
    public Resultado<NotificacionDto> marcarLeida(Long notificacionId) {
        return Ejecucion.conActor(sesion, actor -> notificaciones.marcarLeida(actor, notificacionId));
    }
}
