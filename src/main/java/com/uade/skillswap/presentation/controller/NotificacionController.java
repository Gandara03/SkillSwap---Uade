package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.port.in.NotificacionUseCase;
import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Adaptador delgado: valida la entrada y la traduce a NotificacionUseCase. */
public class NotificacionController implements INotificacionController {

    private final NotificacionUseCase notificaciones;
    private final SesionUseCase sesion;

    public NotificacionController(NotificacionUseCase notificaciones, SesionUseCase sesion) {
        this.notificaciones = notificaciones;
        this.sesion = sesion;
    }

    @Override
    public Resultado<Pagina<NotificacionDto>> listar(boolean soloNoLeidas, PaginaRequest pagina) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirPagina(pagina);
            return notificaciones.listar(actor, soloNoLeidas, pagina);
        });
    }

    @Override
    public Resultado<NotificacionDto> marcarLeida(Long notificacionId) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(notificacionId, "la notificacion");
            return notificaciones.marcarLeida(actor, notificacionId);
        });
    }
}
