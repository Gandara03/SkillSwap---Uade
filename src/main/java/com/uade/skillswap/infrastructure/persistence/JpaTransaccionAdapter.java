package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.application.port.out.TransaccionPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.function.Supplier;

/**
 * Adapter del puerto {@link TransaccionPort} sobre JPA (patron Adapter). Abre la unidad de trabajo del thread
 * (un EntityManager + una transaccion) y la confirma o revierte en conjunto. Es reentrante: si ya hay una
 * unidad activa, el trabajo participa de ella y la decision de commit/rollback la toma el nivel externo.
 * Un conflicto de version (optimistic locking) se traduce a ConflictoConcurrenciaException.
 */
public class JpaTransaccionAdapter implements TransaccionPort {

    private final JpaContexto contexto;

    public JpaTransaccionAdapter(JpaContexto contexto) {
        this.contexto = contexto;
    }

    @Override
    public <T> T ejecutar(Supplier<T> trabajo) {
        if (contexto.hayUnidadActiva()) {
            return trabajo.get();
        }
        EntityManager em = contexto.abrirUnidad();
        EntityTransaction tx = null;
        try {
            tx = em.getTransaction();
            tx.begin();
            T resultado = trabajo.get();
            tx.commit();
            return resultado;
        } catch (RuntimeException e) {
            ErroresJpa.revertirSilencioso(tx);
            throw ErroresJpa.traducir(e);
        } finally {
            contexto.cerrarUnidad();
        }
    }
}
