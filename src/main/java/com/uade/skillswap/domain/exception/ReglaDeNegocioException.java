package com.uade.skillswap.domain.exception;

/** Se viola una regla de negocio o invariante (dato invalido, regla incumplida). */
public class ReglaDeNegocioException extends DominioException {

    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
