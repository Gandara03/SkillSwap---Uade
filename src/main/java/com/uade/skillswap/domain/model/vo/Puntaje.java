package com.uade.skillswap.domain.model.vo;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;

/** Value Object: puntaje de una calificacion, de 1 a 5. */
public final class Puntaje {

    public static final int MINIMO = 1;
    public static final int MAXIMO = 5;

    private final int valor;

    private Puntaje(int valor) {
        this.valor = valor;
    }

    public static Puntaje de(int valor) {
        if (valor < MINIMO || valor > MAXIMO) {
            throw new ReglaDeNegocioException("El puntaje debe estar entre " + MINIMO + " y " + MAXIMO);
        }
        return new Puntaje(valor);
    }

    public int getValor() {
        return valor;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Puntaje p && p.valor == valor;
    }

    @Override
    public int hashCode() {
        return valor;
    }

    @Override
    public String toString() {
        return String.valueOf(valor);
    }
}
