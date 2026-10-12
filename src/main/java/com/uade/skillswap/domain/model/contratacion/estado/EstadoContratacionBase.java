package com.uade.skillswap.domain.model.contratacion.estado;

import com.uade.skillswap.domain.exception.TransicionInvalidaException;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.historial.ContextoTransicion;

/** Rechaza por defecto toda accion; cada estado concreto habilita solo las suyas. */
public abstract class EstadoContratacionBase implements EstadoContratacion {

    @Override
    public void aceptar(Contratacion c, ContextoTransicion t) {
        throw new TransicionInvalidaException("contratacion", getNombre(), "aceptar");
    }

    @Override
    public void rechazar(Contratacion c, ContextoTransicion t) {
        throw new TransicionInvalidaException("contratacion", getNombre(), "rechazar");
    }

    @Override
    public void pagar(Contratacion c, ContextoTransicion t) {
        throw new TransicionInvalidaException("contratacion", getNombre(), "pagar");
    }

    @Override
    public void iniciar(Contratacion c, ContextoTransicion t) {
        throw new TransicionInvalidaException("contratacion", getNombre(), "iniciar");
    }

    @Override
    public void completar(Contratacion c, ContextoTransicion t) {
        throw new TransicionInvalidaException("contratacion", getNombre(), "completar");
    }

    @Override
    public void cancelar(Contratacion c, ContextoTransicion t) {
        throw new TransicionInvalidaException("contratacion", getNombre(), "cancelar");
    }

    @Override
    public boolean permiteReprogramacion() {
        return false;
    }

    @Override
    public boolean esTerminal() {
        return false;
    }
}
