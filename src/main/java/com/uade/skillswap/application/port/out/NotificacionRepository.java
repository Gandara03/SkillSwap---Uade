package com.uade.skillswap.application.port.out;

import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

import java.util.Optional;

public interface NotificacionRepository {
    Notificacion guardar(Notificacion notificacion);
    Optional<Notificacion> buscarPorId(Long id);
    Pagina<Notificacion> buscarPorDestinatario(Long destinatarioId, boolean soloNoLeidas, PaginaRequest pagina);
}
