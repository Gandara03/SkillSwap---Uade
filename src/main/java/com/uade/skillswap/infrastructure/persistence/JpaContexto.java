package com.uade.skillswap.infrastructure.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.util.function.Function;

/**
 * Punto unico de acceso a JPA.
 * Guarda el EntityManagerFactory y el EntityManager de la unidad de trabajo del thread actual:
 * la abre {@link JpaTransaccionAdapter}; los repositorios la reutilizan si existe y, si no, abren un
 * EntityManager efimero por llamada (lectura sin transaccion, escritura con transaccion propia).
 */
public final class JpaContexto implements AutoCloseable {

    private final EntityManagerFactory emf;
    private final Runnable alCerrar;
    private final ThreadLocal<EntityManager> unidad = new ThreadLocal<>();

    /** {@code alCerrar} libera recursos externos al EMF (p. ej. apagar la base H2 en memoria). */
    public JpaContexto(EntityManagerFactory emf, Runnable alCerrar) {
        this.emf = emf;
        this.alCerrar = alCerrar;
    }

    public EntityManagerFactory emf() {
        return emf;
    }

    public boolean hayUnidadActiva() {
        EntityManager em = unidad.get();
        return em != null && em.isOpen();
    }

    /** Abre la unidad de trabajo del thread (la usa el adapter de transacciones). */
    EntityManager abrirUnidad() {
        if (hayUnidadActiva()) {
            throw new IllegalStateException("Ya hay una unidad de trabajo abierta en este thread");
        }
        EntityManager em = emf.createEntityManager();
        unidad.set(em);
        return em;
    }

    /** Cierra la unidad de trabajo del thread (el EntityManager y su cache de primer nivel). */
    void cerrarUnidad() {
        EntityManager em = unidad.get();
        unidad.remove();
        if (em != null && em.isOpen()) {
            em.close();
        }
    }

    /** Ejecuta una lectura: usa la unidad activa o un EntityManager efimero (sin transaccion) que cierra al terminar. */
    public <R> R leer(Function<EntityManager, R> trabajo) {
        if (hayUnidadActiva()) {
            return trabajo.apply(unidad.get());
        }
        EntityManager em = emf.createEntityManager();
        try {
            return trabajo.apply(em);
        } finally {
            em.close();
        }
    }

    /**
     * Ejecuta una escritura. Dentro de una unidad participa de su transaccion (confirma el adapter); fuera,
     * abre un EntityManager con transaccion propia: commit si sale bien, rollback si falla.
     */
    public <R> R escribir(Function<EntityManager, R> trabajo) {
        if (hayUnidadActiva()) {
            return trabajo.apply(unidad.get());
        }
        EntityManager em = emf.createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            R resultado = trabajo.apply(em);
            tx.commit();
            return resultado;
        } catch (RuntimeException e) {
            ErroresJpa.revertirSilencioso(tx);
            throw ErroresJpa.traducir(e);
        } finally {
            em.close();
        }
    }

    /** Cierra el EntityManagerFactory (y lo asociado, como la base en memoria). */
    public void cerrar() {
        try {
            if (emf.isOpen()) {
                emf.close();
            }
        } finally {
            alCerrar.run();
        }
    }

    @Override
    public void close() {
        cerrar();
    }
}
