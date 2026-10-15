package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.acceptance.jpa.AceptacionJpaBase;
import com.uade.skillswap.application.exception.ErrorCode;
import com.uade.skillswap.presentation.Resultado;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base de los tests de integracion de los controllers. Hereda de AceptacionJpaBase: cada test arranca una base H2
 * nueva (Flyway + hbm2ddl=validate) y arma el sistema completo con Composicion, asi que cada llamada recorre el
 * camino real: controller -> ValidadorEntrada -> Ejecucion -> caso de uso -> repositorio JPA -> H2.
 * Agrega lo necesario para revisar errores: el codigo esperado y una sesion cerrada.
 */
abstract class ControllerJpaTestBase extends AceptacionJpaBase {

    /** Exige que la operacion haya fallado con ese codigo y devuelve el mensaje, por si el test quiere revisarlo. */
    protected static String falla(ErrorCode esperado, Resultado<?> resultado) {
        assertTrue(resultado.esError(), () -> "Se esperaba " + esperado + " pero fue: " + resultado);
        assertEquals(esperado, resultado.codigo(), () -> "Mensaje: " + resultado.mensaje());
        return resultado.mensaje();
    }

    /** Nadie con sesion iniciada: los controllers que necesitan actor responden NO_AUTORIZADO. */
    protected void sinSesion() {
        identidad.cerrarSesion();
    }

    /** Texto de exactamente esa cantidad de caracteres (para probar los largos maximos). */
    protected static String texto(int caracteres) {
        return "x".repeat(caracteres);
    }
}
