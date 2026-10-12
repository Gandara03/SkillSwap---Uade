package com.uade.skillswap.application.service;

import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.exception.RecursoNoEncontradoException;
import com.uade.skillswap.application.mapper.Mapeador;
import com.uade.skillswap.application.port.in.NotificacionUseCase;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.NotificacionRepository;

/** Bandeja de notificaciones del actor. */
public class NotificacionService implements NotificacionUseCase {

    private final NotificacionRepository notificacionRepository;

    public NotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Override
    public Pagina<NotificacionDto> listar(Long actorId, boolean soloNoLeidas, PaginaRequest pagina) {
        Entradas.exigir(actorId, "El actor");
        Entradas.exigirPagina(pagina);
        return notificacionRepository.buscarPorDestinatario(actorId, soloNoLeidas, pagina)
                .mapear(Mapeador::notificacion);
    }

    @Override
    public NotificacionDto marcarLeida(Long actorId, Long notificacionId) {
        Entradas.exigir(actorId, "El actor");
        Entradas.exigir(notificacionId, "La notificacion");
        Notificacion notificacion = notificacionRepository.buscarPorId(notificacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Notificacion", notificacionId));
        if (!notificacion.getDestinatarioId().equals(actorId)) {
            throw new NoAutorizadoException("La notificacion pertenece a otro perfil");
        }
        notificacion.marcarLeida();
        return Mapeador.notificacion(notificacionRepository.guardar(notificacion));
    }
}
