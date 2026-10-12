package com.uade.skillswap.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Movimiento de la wallet. Solo se insertan filas (inmutable). */
@Entity
@Table(name = "movimiento_credito")
public class MovimientoCreditoJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "perfil_id", nullable = false, updatable = false)
    private Long perfilId;

    @Column(name = "tipo", length = 20, nullable = false, updatable = false)
    private String tipo;

    @Column(name = "monto", precision = 19, scale = 2, nullable = false, updatable = false)
    private BigDecimal monto;

    @Column(name = "saldo_posterior", precision = 19, scale = 2, nullable = false, updatable = false)
    private BigDecimal saldoPosterior;

    @Column(name = "contratacion_id", updatable = false)
    private Long contratacionId;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;

    public MovimientoCreditoJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPerfilId() {
        return perfilId;
    }

    public void setPerfilId(Long perfilId) {
        this.perfilId = perfilId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public BigDecimal getSaldoPosterior() {
        return saldoPosterior;
    }

    public void setSaldoPosterior(BigDecimal saldoPosterior) {
        this.saldoPosterior = saldoPosterior;
    }

    public Long getContratacionId() {
        return contratacionId;
    }

    public void setContratacionId(Long contratacionId) {
        this.contratacionId = contratacionId;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }
}
