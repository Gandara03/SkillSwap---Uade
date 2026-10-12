package com.uade.skillswap.infrastructure.notificacion;

import com.uade.skillswap.application.port.out.CanalNotificacionPort;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.model.perfil.Perfil;

/**
 * Canal interno: la entrega es la propia bandeja de la aplicacion, que ya queda persistida por el
 * observer, asi que no hay sistema externo al que llamar.
 */
public class InternaCanalAdapter implements CanalNotificacionPort {

    @Override
    public CanalNotificacion getCanal() {
        return CanalNotificacion.INTERNA;
    }

    @Override
    public void enviar(Notificacion notificacion, Perfil destinatario) {
        // Intencionalmente vacio: la notificacion ya esta en la bandeja del destinatario.
    }
}
