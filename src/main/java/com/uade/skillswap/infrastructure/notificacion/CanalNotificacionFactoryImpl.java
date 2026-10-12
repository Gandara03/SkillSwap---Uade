package com.uade.skillswap.infrastructure.notificacion;

import com.uade.skillswap.application.port.out.CanalNotificacionFactory;
import com.uade.skillswap.application.port.out.CanalNotificacionPort;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** Simple Factory: devuelve el adapter del canal pedido. Si el canal es null usa el interno. */
public class CanalNotificacionFactoryImpl implements CanalNotificacionFactory {

    private final Map<CanalNotificacion, CanalNotificacionPort> canales = new EnumMap<>(CanalNotificacion.class);

    public CanalNotificacionFactoryImpl(CanalNotificacionPort email, CanalNotificacionPort push,
                                        CanalNotificacionPort interna) {
        canales.put(CanalNotificacion.EMAIL, Objects.requireNonNull(email, "email"));
        canales.put(CanalNotificacion.PUSH, Objects.requireNonNull(push, "push"));
        canales.put(CanalNotificacion.INTERNA, Objects.requireNonNull(interna, "interna"));
    }

    @Override
    public CanalNotificacionPort crear(CanalNotificacion canal) {
        return canales.get(canal == null ? CanalNotificacion.INTERNA : canal);
    }
}
