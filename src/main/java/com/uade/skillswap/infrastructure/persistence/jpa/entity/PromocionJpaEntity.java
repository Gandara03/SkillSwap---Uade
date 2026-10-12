package com.uade.skillswap.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Promocion de una publicacion (solo cambia 'activa'). */
@Entity
@Table(name = "promocion")
public class PromocionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "publicacion_id", nullable = false, updatable = false)
    private Long publicacionId;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "tipo", length = 20, nullable = false, updatable = false)
    private String tipo;

    @Column(name = "valor", precision = 19, scale = 4, nullable = false, updatable = false)
    private BigDecimal valor;

    @Column(name = "vigente_desde", updatable = false)
    private LocalDateTime vigenteDesde;

    @Column(name = "vigente_hasta", updatable = false)
    private LocalDateTime vigenteHasta;

    @Column(name = "activa", nullable = false)
    private boolean activa;

    public PromocionJpaEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPublicacionId() {
        return publicacionId;
    }

    public void setPublicacionId(Long publicacionId) {
        this.publicacionId = publicacionId;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public LocalDateTime getVigenteDesde() {
        return vigenteDesde;
    }

    public void setVigenteDesde(LocalDateTime vigenteDesde) {
        this.vigenteDesde = vigenteDesde;
    }

    public LocalDateTime getVigenteHasta() {
        return vigenteHasta;
    }

    public void setVigenteHasta(LocalDateTime vigenteHasta) {
        this.vigenteHasta = vigenteHasta;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }
}
