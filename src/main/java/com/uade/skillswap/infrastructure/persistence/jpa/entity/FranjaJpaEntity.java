package com.uade.skillswap.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/** Franja de disponibilidad de una publicacion (una sola reserva por franja). */
@Entity
@Table(name = "franja")
public class FranjaJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private PublicacionJpaEntity publicacion;

    @Column(name = "inicio", nullable = false, updatable = false)
    private LocalDateTime inicio;

    @Column(name = "fin", nullable = false, updatable = false)
    private LocalDateTime fin;

    @Column(name = "ocupada", nullable = false)
    private boolean ocupada;

    public FranjaJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public PublicacionJpaEntity getPublicacion() {
        return publicacion;
    }

    public void setPublicacion(PublicacionJpaEntity publicacion) {
        this.publicacion = publicacion;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public void setFin(LocalDateTime fin) {
        this.fin = fin;
    }

    public boolean isOcupada() {
        return ocupada;
    }

    public void setOcupada(boolean ocupada) {
        this.ocupada = ocupada;
    }
}
