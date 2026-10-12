package com.uade.skillswap.domain.model.contratacion.observer;

/** Observer Pattern - Subject. */
public interface ContratacionSubject {

    void agregarObserver(ContratacionObserver observer);

    void quitarObserver(ContratacionObserver observer);

    void notificarObservers(ContratacionEvent evento);
}
