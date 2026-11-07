package com.uade.skillswap.domain.model.politica;

/** Simple Factory: construye la estrategia a partir del tipo guardado en la publicacion. */
public final class PoliticaCancelacionFactory {

    private final UmbralesCancelacion flexible;
    private final UmbralesCancelacion estricta;

    public PoliticaCancelacionFactory() {
        this(PoliticaFlexible.UMBRALES_POR_DEFECTO, PoliticaEstricta.UMBRALES_POR_DEFECTO);
    }

    public PoliticaCancelacionFactory(UmbralesCancelacion flexible, UmbralesCancelacion estricta) {
        this.flexible = flexible;
        this.estricta = estricta;
    }

    public PoliticaCancelacion crear(TipoPoliticaCancelacion tipo) {
        if (tipo == null) {
            return new PoliticaFlexible(flexible);
        }
        return switch (tipo) {
            case FLEXIBLE -> new PoliticaFlexible(flexible);
            case ESTRICTA -> new PoliticaEstricta(estricta);
        };
    }
}
