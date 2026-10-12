package com.uade.skillswap.infrastructure.notificacion;

import com.uade.skillswap.application.port.out.CanalNotificacionPort;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.infrastructure.notificacion.sdk.PushSdkMock;

/** Adapter: adapta {@link PushSdkMock#pushear} al puerto {@link CanalNotificacionPort}. */
public class PushCanalAdapter implements CanalNotificacionPort {

    private final PushSdkMock sdk;

    public PushCanalAdapter(PushSdkMock sdk) {
        this.sdk = sdk;
    }

    @Override
    public CanalNotificacion getCanal() {
        return CanalNotificacion.PUSH;
    }

    @Override
    public void enviar(Notificacion notificacion, Perfil destinatario) {
        sdk.pushear("dispositivo-" + destinatario.getUserId(), "SkillSwap", notificacion.getMensaje());
    }
}
