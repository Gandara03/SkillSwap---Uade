package com.uade.skillswap.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Historial de estados de una publicacion. */
@Entity
@Table(name = "historial_publicacion")
public class HistorialPublicacionJpaEntity extends HistorialBaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private PublicacionJpaEntity publicacion;

    public HistorialPublicacionJpaEntity() {
    }

    public PublicacionJpaEntity getPublicacion() {
        return publicacion;
    }

    public void setPublicacion(PublicacionJpaEntity publicacion) {
        this.publicacion = publicacion;
    }
}
