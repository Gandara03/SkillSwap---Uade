package com.uade.skillswap.application.port.out;

import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.model.perfil.Perfil;

/**
 * Canal de entrega de una notificacion. Es el Target de los adapters de email y push; el canal interno lo
 * implementa sin Adaptado.
 */
public interface CanalNotificacionPort {
    CanalNotificacion getCanal();
    void enviar(Notificacion notificacion, Perfil destinatario);
}
