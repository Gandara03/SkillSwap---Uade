package com.uade.skillswap.domain.model.politica;

/** Simple Factory: construye la estrategia a partir del tipo guardado en la publicacion. */
public final class PoliticaCancelacionFactory {

    private final ConfiguracionCancelacion configuracion;

    public PoliticaCancelacionFactory() {
        this(ConfiguracionCancelacion.porDefecto());
    }

    public PoliticaCancelacionFactory(ConfiguracionCancelacion configuracion) {
        this.configuracion = configuracion;
    }

    public PoliticaCancelacion crear(TipoPoliticaCancelacion tipo) {
        if (tipo == null) {
            return new PoliticaFlexible(configuracion.flexible());
        }
        return switch (tipo) {
            case FLEXIBLE -> new PoliticaFlexible(configuracion.flexible());
            case ESTRICTA -> new PoliticaEstricta(configuracion.estricta());
        };
    }

    public PoliticaCancelacion crear(String nombre) {
        return crear(TipoPoliticaCancelacion.valueOf(nombre.trim().toUpperCase()));
    }
}
