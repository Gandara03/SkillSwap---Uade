package com.uade.skillswap.acceptance.memoria;

import com.uade.skillswap.application.dto.CalificacionDto;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.exception.TransicionInvalidaException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Caso de aceptacion 10: completar una contratacion, calificarla una sola vez y recalcular la reputacion.
 * La reputacion nunca se edita: es el promedio de las calificaciones recibidas, recalculado en cada calificacion.
 */
@DisplayName("Caso 10 (memoria): completar, calificar una sola vez y recalcular reputacion")
class Caso10CompletarCalificarYReputacionTest {

    private static BigDecimal sc(long monto) {
        return BigDecimal.valueOf(monto);
    }

    @Test
    @DisplayName("Caso 10: completar acredita al proveedor, la calificacion se acepta una vez y la reputacion es el promedio")
    void caso10_completarCalificarUnaSolaVezYRecalcularReputacion() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PerfilDto cecilia = e.profesional("u-cecilia", "Cecilia");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 40, 2, 2);

        ContratacionDto deBruno = e.contratarYCompletar(ana.id(), bruno.id(), mentoria, 0);
        ContratacionDto deCecilia = e.contratarYCompletar(ana.id(), cecilia.id(), mentoria, 1);
        assertEquals("COMPLETADA", deBruno.estado());
        // completar libera lo retenido: Ana cobra 40 + 40
        assertEquals(0, sc(180).compareTo(e.saldo(ana.id())));
        assertEquals(0.0, e.app.perfilUseCase.obtener(ana.id()).reputacion(), 0.0001, "sin calificaciones");

        // Bruno califica con 5: reputacion 5.0
        CalificacionDto primera = e.app.calificacionUseCase.calificar(bruno.id(), e.calificacion(deBruno.id(), 5));
        assertEquals(ana.id(), primera.destinatarioId());
        assertEquals(bruno.id(), primera.autorId());
        assertEquals(5.0, e.app.perfilUseCase.obtener(ana.id()).reputacion(), 0.0001);

        // el mismo autor no puede calificar dos veces la misma contratacion; la reputacion no se mueve
        assertThrows(ReglaDeNegocioException.class,
                () -> e.app.calificacionUseCase.calificar(bruno.id(), e.calificacion(deBruno.id(), 1)));
        PerfilDto tras = e.app.perfilUseCase.obtener(ana.id());
        assertEquals(5.0, tras.reputacion(), 0.0001);
        assertEquals(1, tras.cantidadCalificaciones());
        assertEquals(1L, e.app.calificacionUseCase.listarRecibidas(ana.id(), e.primeraPagina()).totalElementos());

        // Cecilia califica con 3: la reputacion pasa a ser el promedio de las recibidas (5 y 3) = 4.0
        e.app.calificacionUseCase.calificar(cecilia.id(), e.calificacion(deCecilia.id(), 3));
        PerfilDto promedio = e.app.perfilUseCase.obtener(ana.id());
        assertEquals(4.0, promedio.reputacion(), 0.0001);
        assertEquals(2, promedio.cantidadCalificaciones());
    }

    @Test
    @DisplayName("Caso 10: ambas partes pueden calificarse entre si, una vez cada una, y cada reputacion es independiente")
    void caso10_cadaParteCalificaUnaVezALaOtra() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 40, 1, 1);
        ContratacionDto completada = e.contratarYCompletar(ana.id(), bruno.id(), mentoria, 0);

        e.app.calificacionUseCase.calificar(bruno.id(), e.calificacion(completada.id(), 4));
        e.app.calificacionUseCase.calificar(ana.id(), e.calificacion(completada.id(), 2));

        assertEquals(4.0, e.app.perfilUseCase.obtener(ana.id()).reputacion(), 0.0001);
        assertEquals(2.0, e.app.perfilUseCase.obtener(bruno.id()).reputacion(), 0.0001);
        assertThrows(ReglaDeNegocioException.class,
                () -> e.app.calificacionUseCase.calificar(ana.id(), e.calificacion(completada.id(), 5)));
        assertEquals(2.0, e.app.perfilUseCase.obtener(bruno.id()).reputacion(), 0.0001);
    }

    @Test
    @DisplayName("Caso 10: no se puede calificar una contratacion que todavia no esta completada")
    void caso10_noSeCalificaAntesDeCompletar() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 40, 1, 1);
        ContratacionDto pagada = e.contratarYPagar(ana.id(), bruno.id(), mentoria, 0);

        Throwable error = assertThrows(RuntimeException.class,
                () -> e.app.calificacionUseCase.calificar(bruno.id(), e.calificacion(pagada.id(), 5)));
        assertEquals(true, error instanceof ReglaDeNegocioException || error instanceof TransicionInvalidaException,
                error.getClass().getName());
        assertEquals(0.0, e.app.perfilUseCase.obtener(ana.id()).reputacion(), 0.0001);
    }
}
