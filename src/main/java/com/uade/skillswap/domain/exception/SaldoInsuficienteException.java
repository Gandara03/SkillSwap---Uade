package com.uade.skillswap.domain.exception;

/** El saldo de SkillCoins no alcanza para la operacion. */
public class SaldoInsuficienteException extends DominioException {

    public SaldoInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
