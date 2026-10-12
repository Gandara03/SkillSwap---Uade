package com.uade.skillswap.infrastructure.persistence.jpa.mapper;

import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.NotificacionJpaEntity;

/** Notificacion (dominio) <-> NotificacionJpaEntity. */
public final class NotificacionMapper {

    private NotificacionMapper() {
    }

    public static Notificacion aDominio(NotificacionJpaEntity e) {
        return Notificacion.reconstruir(e.getId(), e.getDestinatarioId(), TipoEventoContratacion.valueOf(e.getTipoEvento()),
                e.getMensaje(), CanalNotificacion.valueOf(e.getCanal()), e.isLeida(), e.getFecha());
    }

    public static void aplicar(Notificacion d, NotificacionJpaEntity e) {
        e.setDestinatarioId(d.getDestinatarioId());
        e.setTipoEvento(d.getTipoEvento().name());
        e.setMensaje(d.getMensaje());
        e.setCanal(d.getCanal().name());
        e.setLeida(d.estaLeida());
        e.setFecha(Fechas.enMicros(d.getFecha()));
    }

    public static void confirmar(Notificacion d, NotificacionJpaEntity e) {
        CamposDominio.asignar(d, "id", e.getId());
    }
}
