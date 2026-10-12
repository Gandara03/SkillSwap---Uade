package com.uade.skillswap.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;

/** Calificacion inmutable. Una por (contratacion, autor): lo garantiza la restriccion unica de la base. */
@Entity
@Table(name = "calificacion", uniqueConstraints = @UniqueConstraint(name = "uk_calificacion_contratacion_autor", columnNames = {"contratacion_id", "autor_id"}))
public class CalificacionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "contratacion_id", nullable = false, updatable = false)
    private Long contratacionId;

    @Column(name = "autor_id", nullable = false, updatable = false)
    private Long autorId;

    @Column(name = "destinatario_id", nullable = false, updatable = false)
    private Long destinatarioId;

    @Column(name = "puntaje", nullable = false, updatable = false)
    private int puntaje;

    @Column(name = "comentario", length = 1000, updatable = false)
    private String comentario;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    public CalificacionJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getContratacionId() {
        return contratacionId;
    }

    public void setContratacionId(Long contratacionId) {
        this.contratacionId = contratacionId;
    }

    public Long getAutorId() {
        return autorId;
    }

    public void setAutorId(Long autorId) {
        this.autorId = autorId;
    }

    public Long getDestinatarioId() {
        return destinatarioId;
    }

    public void setDestinatarioId(Long destinatarioId) {
        this.destinatarioId = destinatarioId;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public void setPuntaje(int puntaje) {
        this.puntaje = puntaje;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
