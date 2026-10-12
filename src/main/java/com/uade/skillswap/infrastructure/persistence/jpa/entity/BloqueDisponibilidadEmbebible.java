package com.uade.skillswap.infrastructure.persistence.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.time.LocalTime;
import java.util.Objects;

/** Bloque semanal de disponibilidad de un perfil (value object embebido en perfil_disponibilidad). */
@Embeddable
public class BloqueDisponibilidadEmbebible {

    @Column(name = "dia", length = 10, nullable = false)
    private String dia;

    @Column(name = "desde", nullable = false)
    private LocalTime desde;

    @Column(name = "hasta", nullable = false)
    private LocalTime hasta;

    public BloqueDisponibilidadEmbebible() {
    }

    public String getDia() {
        return dia;
    }

    public void setDia(String dia) {
        this.dia = dia;
    }

    public LocalTime getDesde() {
        return desde;
    }

    public void setDesde(LocalTime desde) {
        this.desde = desde;
    }

    public LocalTime getHasta() {
        return hasta;
    }

    public void setHasta(LocalTime hasta) {
        this.hasta = hasta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BloqueDisponibilidadEmbebible otro)) {
            return false;
        }
        return Objects.equals(dia, otro.dia) && Objects.equals(desde, otro.desde) && Objects.equals(hasta, otro.hasta);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dia, desde, hasta);
    }
}
