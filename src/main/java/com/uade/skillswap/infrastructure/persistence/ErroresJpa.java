package com.uade.skillswap.infrastructure.persistence;

import com.uade.skillswap.application.exception.ConflictoConcurrenciaException;
import com.uade.skillswap.application.exception.AplicacionException;
import jakarta.persistence.EntityTransaction;

import java.sql.SQLException;
import java.util.Set;

/** Traduce errores de JPA/Hibernate a errores de la aplicacion (hoy: conflictos de concurrencia). */
public final class ErroresJpa {

    /** Nombres simples (sin importar Hibernate) de las excepciones que indican un conflicto de version o de lock. */
    private static final Set<String> CONFLICTOS = Set.of(
            "OptimisticLockException",
            "StaleObjectStateException",
            "StaleStateException",
            "OptimisticEntityLockException",
            "PessimisticLockException",
            "LockTimeoutException",
            "LockAcquisitionException");

    // Clave duplicada (UNIQUE): SQLState estandar de H2 y codigo de error de MySQL. Las demas violaciones
    // (clave foranea, NOT NULL) no son carreras entre usuarios: siguen siendo errores internos.
    private static final String SQLSTATE_CLAVE_DUPLICADA = "23505";
    private static final int MYSQL_CLAVE_DUPLICADA = 1062;

    private static final int PROFUNDIDAD_MAXIMA = 12;

    private ErroresJpa() {
    }

    /**
     * Devuelve la excepcion a relanzar: {@link ConflictoConcurrenciaException} si la causa (o alguna causa
     * anidada, p. ej. dentro de un RollbackException) es un conflicto de version o una clave duplicada (dos pedidos
     * simultaneos que insertan lo mismo: el segundo choca con el UNIQUE); si no, la misma excepcion.
     */
    public static RuntimeException traducir(RuntimeException error) {
        if (error instanceof AplicacionException) {
            return error;
        }
        if (esConflicto(error)) {
            return new ConflictoConcurrenciaException(
                    "Otro usuario modifico el mismo recurso. Reintenta la operacion.", error);
        }
        return error;
    }

    public static boolean esConflicto(Throwable error) {
        Throwable actual = error;
        for (int i = 0; actual != null && i < PROFUNDIDAD_MAXIMA; i++) {
            if (esClaveDuplicada(actual)) {
                return true;
            }
            for (Class<?> c = actual.getClass(); c != null; c = c.getSuperclass()) {
                if (CONFLICTOS.contains(c.getSimpleName())) {
                    return true;
                }
            }
            actual = actual.getCause() == actual ? null : actual.getCause();
        }
        return false;
    }

    private static boolean esClaveDuplicada(Throwable error) {
        if (!(error instanceof SQLException)) {
            return false;
        }
        SQLException sql = (SQLException) error;
        return SQLSTATE_CLAVE_DUPLICADA.equals(sql.getSQLState()) || sql.getErrorCode() == MYSQL_CLAVE_DUPLICADA;
    }

    /** Rollback que nunca tapa el error original. */
    public static void revertirSilencioso(EntityTransaction tx) {
        try {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        } catch (RuntimeException ignorada) {
            // el error que importa es el original; el rollback fallido no debe reemplazarlo
        }
    }
}
