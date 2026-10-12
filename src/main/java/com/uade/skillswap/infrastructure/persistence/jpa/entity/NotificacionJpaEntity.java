package com.uade.skillswap.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Notificacion en la bandeja de un destinatario (solo cambia 'leida'). */
@Entity
@Table(name = "notificacion")
public class NotificacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "destinatario_id", nullable = false, updatable = false)
    private Long destinatarioId;

    @Column(name = "tipo_evento", length = 40, nullable = false, updatable = false)
    private String tipoEvento;

    @Column(name = "mensaje", length = 1000, nullable = false, updatable = false)
    private String mensaje;

    @Column(name = "canal", length = 20, nullable = false, updatable = false)
    private String canal;

    @Column(name = "leida", nullable = false)
    private boolean leida;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    public NotificacionJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(Long destinatarioId) {
        this.destinatarioId = destinatarioId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getCanal() {
        return canal;
    }

    public void setCanal(String canal) {
        this.canal = canal;
    }

    public boolean isLeida() {
        return leida;
    }

    public void setLeida(boolean leida) {
        this.leida = leida;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
