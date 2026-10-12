package com.uade.skillswap.application.port.out;

import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.model.perfil.Perfil;

/** Target del Adapter: un canal de entrega (email, push, interna). */
public interface CanalNotificacionPort {
    CanalNotificacion getCanal();
    void enviar(Notificacion notificacion, Perfil destinatario);
}
