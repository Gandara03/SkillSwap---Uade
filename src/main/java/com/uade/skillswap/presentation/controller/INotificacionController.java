package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Contrato de la UI para la bandeja de notificaciones del usuario. */
public interface INotificacionController {

    Resultado<Pagina<NotificacionDto>> listar(boolean soloNoLeidas, PaginaRequest pagina);

    Resultado<NotificacionDto> marcarLeida(Long notificacionId);
}
