package com.uade.skillswap.domain.model.publicacion;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;

import java.time.LocalDateTime;

/** Franja de disponibilidad concreta de una publicacion. Una franja admite una sola reserva. */
public class Franja {

    private Long id;
    private final LocalDateTime inicio;
    private final LocalDateTime fin;
    private boolean ocupada;

    public Franja(Long id, LocalDateTime inicio, LocalDateTime fin, boolean ocupada) {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            throw new ReglaDeNegocioException("La franja requiere inicio anterior al fin");
        }
        this.id = id;
        this.inicio = inicio;
        this.fin = fin;
        this.ocupada = ocupada;
    }

    public static Franja nueva(LocalDateTime inicio, LocalDateTime fin) {
        return new Franja(null, inicio, fin, false);
    }

    void ocupar() {
        this.ocupada = true;
    }

    void liberar() {
        this.ocupada = false;
    }

    public boolean yaComenzo(LocalDateTime ahora) {
        return !inicio.isAfter(ahora);
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFin() {
        return fin;
    }

    public boolean estaOcupada() {
        return ocupada;
    }
}
