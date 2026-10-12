package com.uade.skillswap.domain.model.publicacion.estado;

/** CERRADA: finalizada; no puede reabrirse. */
public class EstadoCerrada extends EstadoPublicacionBase {

    @Override
    public String getNombre() {
        return "CERRADA";
    }

    @Override
    public boolean esTerminal() {
        return true;
    }
}
