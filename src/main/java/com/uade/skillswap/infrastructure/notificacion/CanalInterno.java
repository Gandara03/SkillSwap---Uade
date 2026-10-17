package com.uade.skillswap.infrastructure.notificacion;

import com.uade.skillswap.application.port.out.CanalNotificacionPort;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.model.perfil.Perfil;

/**
 * Canal interno: implementa {@link CanalNotificacionPort} sin un sistema externo detras, asi que no es un Adapter.
 * La entrega es la propia bandeja de la aplicacion, que ya guardo el observer.
 */
public class CanalInterno implements CanalNotificacionPort {

    @Override
    public CanalNotificacion getCanal() {
        return CanalNotificacion.INTERNA;
    }

    @Override
    public void enviar(Notificacion notificacion, Perfil destinatario) {
        // Intencionalmente vacio: la notificacion ya esta en la bandeja del destinatario.
    }
}
