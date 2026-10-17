package com.uade.skillswap.infrastructure.notificacion;

import com.uade.skillswap.application.port.out.CanalNotificacionFactory;
import com.uade.skillswap.application.port.out.CanalNotificacionPort;
import com.uade.skillswap.application.port.out.IdentidadPort;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.infrastructure.notificacion.sdk.EmailSdkMock;
import com.uade.skillswap.infrastructure.notificacion.sdk.PushSdkMock;

import java.util.Objects;

/**
 * Simple Factory: crea en cada pedido el canal elegido, el adapter de email o de push o el canal interno (tambien si
 * el canal es null). Solo guarda lo que los adapters necesitan para crearse: el SDK de cada proveedor externo (uno
 * solo por proveedor) y la identidad, que el adapter de email usa para conocer la direccion del usuario.
 */
public class CanalNotificacionFactoryImpl implements CanalNotificacionFactory {

    private final EmailSdkMock emailSdk;
    private final PushSdkMock pushSdk;
    private final IdentidadPort identidad;

    public CanalNotificacionFactoryImpl(EmailSdkMock emailSdk, PushSdkMock pushSdk, IdentidadPort identidad) {
        this.emailSdk = Objects.requireNonNull(emailSdk, "emailSdk");
        this.pushSdk = Objects.requireNonNull(pushSdk, "pushSdk");
        this.identidad = Objects.requireNonNull(identidad, "identidad");
    }

    @Override
    public CanalNotificacionPort crear(CanalNotificacion canal) {
        if (canal == null) {
            return new CanalInterno();
        }
        return switch (canal) {
            case EMAIL -> new EmailCanalAdapter(emailSdk, identidad);
            case PUSH -> new PushCanalAdapter(pushSdk);
            case INTERNA -> new CanalInterno();
        };
    }
}
