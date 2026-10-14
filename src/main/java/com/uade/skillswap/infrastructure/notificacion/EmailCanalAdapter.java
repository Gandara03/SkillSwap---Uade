package com.uade.skillswap.infrastructure.notificacion;

import com.uade.skillswap.application.port.out.CanalNotificacionPort;
import com.uade.skillswap.application.port.out.IdentidadPort;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.infrastructure.notificacion.sdk.EmailSdkMock;

/** Adapter: adapta {@link EmailSdkMock#enviarCorreo} al puerto {@link CanalNotificacionPort}. */
public class EmailCanalAdapter implements CanalNotificacionPort {

    private final EmailSdkMock sdk;
    private final IdentidadPort identidad;

    /** El destino es el email real del usuario segun la identidad; si no lo encuentra, uno armado con su userId. */
    public EmailCanalAdapter(EmailSdkMock sdk, IdentidadPort identidad) {
        this.sdk = sdk;
        this.identidad = identidad;
    }

    @Override
    public CanalNotificacion getCanal() {
        return CanalNotificacion.EMAIL;
    }

    @Override
    public void enviar(Notificacion notificacion, Perfil destinatario) {
        String asunto = "SkillSwap: " + notificacion.getTipoEvento();
        sdk.enviarCorreo(direccionDe(destinatario), asunto, notificacion.getMensaje());
    }

    private String direccionDe(Perfil destinatario) {
        return identidad.buscarPorUserId(destinatario.getUserId())
                .map(IdentidadPort.UsuarioIdentidad::email)
                .orElse(porDefecto(destinatario));
    }

    private static String porDefecto(Perfil destinatario) {
        return destinatario.getUserId() + "@skillswap.local";
    }
}
