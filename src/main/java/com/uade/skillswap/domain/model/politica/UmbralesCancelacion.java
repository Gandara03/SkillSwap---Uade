package com.uade.skillswap.domain.model.politica;

/**
 * Umbrales de una politica de cancelacion: con al menos {@code horasReintegroTotal} de anticipacion se
 * devuelve el 100 %, con al menos {@code horasReintegroParcial} se devuelve {@code porcentajeParcial} %,
 * y por debajo no hay reintegro.
 */
public record UmbralesCancelacion(long horasReintegroTotal, long horasReintegroParcial, int porcentajeParcial) {

    public UmbralesCancelacion {
        if (horasReintegroTotal < horasReintegroParcial) {
            throw new IllegalArgumentException("El umbral total no puede ser menor que el parcial");
        }
        if (porcentajeParcial < 0 || porcentajeParcial > 100) {
            throw new IllegalArgumentException("El porcentaje parcial debe estar entre 0 y 100");
        }
    }
}
