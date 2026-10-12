package com.uade.skillswap.application.port.out;

import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;

/** Simple Factory (puerto): devuelve el canal que corresponde a la preferencia del usuario. */
public interface CanalNotificacionFactory {
    CanalNotificacionPort crear(CanalNotificacion canal);
}
