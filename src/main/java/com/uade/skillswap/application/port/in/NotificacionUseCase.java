package com.uade.skillswap.application.port.in;

import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

public interface NotificacionUseCase {

    Pagina<NotificacionDto> listar(Long actorId, boolean soloNoLeidas, PaginaRequest pagina);

    NotificacionDto marcarLeida(Long actorId, Long notificacionId);
}
