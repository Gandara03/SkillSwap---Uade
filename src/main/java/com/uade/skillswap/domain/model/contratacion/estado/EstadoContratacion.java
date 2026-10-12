package com.uade.skillswap.domain.model.contratacion.estado;

import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.historial.ContextoTransicion;

/**
 * State Pattern - State: acciones validas sobre una Contratacion en cada estado
 * (ver tabla 4.5 de la consigna). Cada accion invalida lanza TransicionInvalidaException.
 */
public interface EstadoContratacion {

    String getNombre();

    void aceptar(Contratacion c, ContextoTransicion t);

    void rechazar(Contratacion c, ContextoTransicion t);

    void pagar(Contratacion c, ContextoTransicion t);

    void iniciar(Contratacion c, ContextoTransicion t);

    void completar(Contratacion c, ContextoTransicion t);

    void cancelar(Contratacion c, ContextoTransicion t);

    /** Reprogramar solo es posible antes de comenzar (ACEPTADA o CONFIRMADA). */
    boolean permiteReprogramacion();

    boolean esTerminal();
}
