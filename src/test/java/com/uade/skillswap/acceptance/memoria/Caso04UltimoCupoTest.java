package com.uade.skillswap.acceptance.memoria;

import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.domain.exception.CupoAgotadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Caso de aceptacion 4: evitar que dos aceptaciones consuman el ultimo cupo.
 * Version en memoria: la regla de cupo (dominio) se verifica en secuencia. La carrera real entre dos
 * transacciones se prueba en la version JPA (optimistic locking con @Version).
 */
@DisplayName("Caso 4 (memoria): evitar que dos aceptaciones consuman el ultimo cupo")
class Caso04UltimoCupoTest {

    private static BigDecimal sc(long monto) {
        return BigDecimal.valueOf(monto);
    }

    @Test
    @DisplayName("Caso 4: con cupo 1 y dos solicitudes, la segunda aceptacion falla y no consume nada")
    void caso04_dosAceptacionesNoConsumenElUltimoCupo() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PerfilDto cecilia = e.profesional("u-cecilia", "Cecilia");
        // cupo 1, dos franjas distintas: las dos solicitudes son validas porque el cupo se consume al ACEPTAR
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 40, 1, 2);
        ContratacionDto deBruno = e.solicitar(bruno.id(), mentoria, 0);
        ContratacionDto deCecilia = e.solicitar(cecilia.id(), mentoria, 1);
        assertEquals("PENDIENTE", deBruno.estado());
        assertEquals("PENDIENTE", deCecilia.estado());

        // la primera aceptacion consume el unico cupo
        assertEquals("ACEPTADA", e.app.contratacionUseCase.aceptar(ana.id(), deBruno.id()).estado());
        assertEquals(0, e.app.publicacionUseCase.obtener(ana.id(), mentoria.id()).cupoDisponible());

        // la segunda falla por cupo agotado
        assertThrows(CupoAgotadoException.class, () -> e.app.contratacionUseCase.aceptar(ana.id(), deCecilia.id()));

        // el cupo no se consumio dos veces: sigue en 0 (no negativo) y solo la franja de Bruno esta ocupada
        PublicacionDto tras = e.app.publicacionUseCase.obtener(ana.id(), mentoria.id());
        assertEquals(0, tras.cupoDisponible());
        assertTrue(tras.franjas().get(0).ocupada());
        assertFalse(tras.franjas().get(1).ocupada(), "la franja de la solicitud rechazada queda libre");

        // los saldos no se tocaron (ninguna de las dos pago) y la aceptacion valida sigue su camino
        assertEquals(0, sc(100).compareTo(e.saldo(bruno.id())));
        assertEquals(0, sc(100).compareTo(e.saldo(cecilia.id())));
        assertEquals(0, sc(100).compareTo(e.saldo(ana.id())));
        assertEquals("CONFIRMADA", e.app.contratacionUseCase.pagar(bruno.id(), deBruno.id()).estado());
        assertEquals(0, sc(60).compareTo(e.saldo(bruno.id())));
        assertEquals(0, sc(100).compareTo(e.saldo(cecilia.id())));
    }

    @Test
    @DisplayName("Caso 4: agotado el cupo, tampoco se admiten nuevas solicitudes")
    void caso04_conElCupoAgotadoNoSeAdmitenSolicitudesNuevas() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PerfilDto cecilia = e.profesional("u-cecilia", "Cecilia");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 40, 1, 2);
        ContratacionDto deBruno = e.solicitar(bruno.id(), mentoria, 0);
        e.app.contratacionUseCase.aceptar(ana.id(), deBruno.id());

        assertThrows(CupoAgotadoException.class, () -> e.solicitar(cecilia.id(), mentoria, 1));
        assertEquals(0L, e.app.contratacionUseCase.listarMias(cecilia.id(), e.primeraPagina()).totalElementos());
    }

    @Test
    @DisplayName("Caso 4: con cupo 2 las dos aceptaciones entran y la tercera ya no")
    void caso04_cupoDeDosAdmiteDosAceptaciones() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PerfilDto cecilia = e.profesional("u-cecilia", "Cecilia");
        PerfilDto dario = e.profesional("u-dario", "Dario");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 40, 2, 3);
        ContratacionDto c1 = e.solicitar(bruno.id(), mentoria, 0);
        ContratacionDto c2 = e.solicitar(cecilia.id(), mentoria, 1);
        ContratacionDto c3 = e.solicitar(dario.id(), mentoria, 2);

        e.app.contratacionUseCase.aceptar(ana.id(), c1.id());
        e.app.contratacionUseCase.aceptar(ana.id(), c2.id());
        assertThrows(CupoAgotadoException.class, () -> e.app.contratacionUseCase.aceptar(ana.id(), c3.id()));
        assertEquals(0, e.app.publicacionUseCase.obtener(ana.id(), mentoria.id()).cupoDisponible());
    }
}
