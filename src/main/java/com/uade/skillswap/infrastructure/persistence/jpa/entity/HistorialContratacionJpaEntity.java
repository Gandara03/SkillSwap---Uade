package com.uade.skillswap.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Historial de estados de una contratacion. */
@Entity
@Table(name = "historial_contratacion")
public class HistorialContratacionJpaEntity extends HistorialBaseJpaEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contratacion_id", nullable = false)
    private ContratacionJpaEntity contratacion;

    public HistorialContratacionJpaEntity() {
    }

    public ContratacionJpaEntity getContratacion() {
        return contratacion;
    }

    public void setContratacion(ContratacionJpaEntity contratacion) {
        this.contratacion = contratacion;
    }
}
