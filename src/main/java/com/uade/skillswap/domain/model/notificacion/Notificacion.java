package com.uade.skillswap.domain.model.notificacion;

import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;

import java.time.LocalDateTime;

/** Notificacion registrada para un destinatario (queda en su bandeja con el canal por el que se envio). */
public class Notificacion {

    private Long id;
    private final Long destinatarioId;
    private final TipoEventoContratacion tipoEvento;
    private final String mensaje;
    private final CanalNotificacion canal;
    private boolean leida;
    private final LocalDateTime fecha;

    private Notificacion(Long id, Long destinatarioId, TipoEventoContratacion tipoEvento, String mensaje,
                         CanalNotificacion canal, boolean leida, LocalDateTime fecha) {
        this.id = id;
        this.destinatarioId = destinatarioId;
        this.tipoEvento = tipoEvento;
        this.mensaje = mensaje;
        this.canal = canal;
        this.leida = leida;
        this.fecha = fecha;
    }

    public static Notificacion crear(Long destinatarioId, TipoEventoContratacion tipoEvento, String mensaje,
                                     CanalNotificacion canal, LocalDateTime ahora) {
        return new Notificacion(null, destinatarioId, tipoEvento, mensaje, canal, false, ahora);
    }

    /** Solo para rehidratar desde persistencia. */
    public static Notificacion reconstruir(Long id, Long destinatarioId, TipoEventoContratacion tipoEvento,
                                           String mensaje, CanalNotificacion canal, boolean leida, LocalDateTime fecha) {
        return new Notificacion(id, destinatarioId, tipoEvento, mensaje, canal, leida, fecha);
    }

    public void marcarLeida() {
        this.leida = true;
    }

    public Long getId() {
        return id;
    }

    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public TipoEventoContratacion getTipoEvento() {
        return tipoEvento;
    }

    public String getMensaje() {
        return mensaje;
    }

    public CanalNotificacion getCanal() {
        return canal;
    }

    public boolean estaLeida() {
        return leida;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }
}
