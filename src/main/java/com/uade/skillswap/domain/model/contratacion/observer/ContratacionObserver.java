package com.uade.skillswap.domain.model.contratacion.observer;

/** Observer Pattern - Observer: interesado en los eventos de una contratacion. */
public interface ContratacionObserver {

    void actualizar(ContratacionEvent evento);
}
