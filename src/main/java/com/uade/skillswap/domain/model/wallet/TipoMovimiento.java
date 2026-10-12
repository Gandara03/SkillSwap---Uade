package com.uade.skillswap.domain.model.wallet;

public enum TipoMovimiento {
    BONO_INICIAL(true),
    PAGO(false),
    COBRO(true),
    REINTEGRO(true);

    private final boolean credito;

    TipoMovimiento(boolean credito) {
        this.credito = credito;
    }

    public boolean esCredito() {
        return credito;
    }
}
