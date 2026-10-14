package com.uade.skillswap.acceptance.memoria;

import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.wallet.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Caso de aceptacion 3: solicitar, aceptar y pagar una mentoria con saldo suficiente. */
@DisplayName("Caso 3 (memoria): solicitar, aceptar y pagar una mentoria con saldo suficiente")
class Caso03SolicitarAceptarYPagarTest {

    private static BigDecimal sc(long monto) {
        return BigDecimal.valueOf(monto);
    }

    @Test
    @DisplayName("Caso 3: PENDIENTE -> ACEPTADA -> CONFIRMADA, el cliente paga y el monto queda retenido")
    void caso03_solicitarAceptarYPagarConSaldoSuficiente() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 40, 1, 1);
        assertEquals(0, sc(100).compareTo(e.saldo(bruno.id())), "bono inicial");

        ContratacionDto solicitud = e.solicitar(bruno.id(), mentoria, 0);
        assertEquals("PENDIENTE", solicitud.estado());
        assertEquals(0, sc(40).compareTo(solicitud.precioFinal()));
        assertEquals(0, sc(100).compareTo(e.saldo(bruno.id())), "solicitar no cobra");

        ContratacionDto aceptada = e.app.contratacionUseCase.aceptar(ana.id(), solicitud.id());
        assertEquals("ACEPTADA", aceptada.estado());
        // aceptar reserva la franja y consume el cupo en la misma operacion
        PublicacionDto tras = e.app.publicacionUseCase.obtener(ana.id(), mentoria.id());
        assertEquals(0, tras.cupoDisponible());
        assertTrue(tras.franjas().get(0).ocupada());
        assertEquals(0, sc(100).compareTo(e.saldo(bruno.id())), "aceptar tampoco cobra");

        ContratacionDto pagada = e.app.contratacionUseCase.pagar(bruno.id(), solicitud.id());
        assertEquals("CONFIRMADA", pagada.estado());
        assertNotNull(pagada.pagadaEn());
        assertEquals(0, sc(60).compareTo(e.saldo(bruno.id())), "100 - 40");
        // escrow: el proveedor todavia no cobra, el monto queda retenido hasta completar la sesion
        assertEquals(0, sc(100).compareTo(e.saldo(ana.id())));

        List<MovimientoDto> movimientos = e.app.walletUseCase.movimientos(bruno.id(), e.primeraPagina()).contenido();
        MovimientoDto ultimo = movimientos.get(0);
        assertEquals(TipoMovimiento.PAGO, ultimo.tipo());
        assertEquals(0, sc(40).compareTo(ultimo.monto()));
        assertEquals(0, sc(60).compareTo(ultimo.saldoResultante()));
        assertEquals(solicitud.id(), ultimo.contratacionId());

        // historial de estados: PENDIENTE -> ACEPTADA -> CONFIRMADA
        List<String> destinos = e.app.contratacionUseCase.historial(bruno.id(), solicitud.id(), e.primeraPagina()).contenido().stream()
                .map(h -> h.estadoNuevo()).toList();
        assertEquals(List.of("PENDIENTE", "ACEPTADA", "CONFIRMADA"), destinos);
    }

    @Test
    @DisplayName("Caso 3: el proveedor es avisado de la solicitud y del pago; el cliente de la aceptacion")
    void caso03_losEventosNotificanALaContraparte() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana", CanalNotificacion.INTERNA);
        PerfilDto bruno = e.profesional("u-bruno", "Bruno", CanalNotificacion.INTERNA);
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 40, 1, 1);

        e.contratarYPagar(ana.id(), bruno.id(), mentoria, 0);

        List<TipoEventoContratacion> paraAna = e.app.notificacionUseCase.listar(ana.id(), false, e.primeraPagina())
                .contenido().stream().map(NotificacionDto::tipoEvento).toList();
        List<TipoEventoContratacion> paraBruno = e.app.notificacionUseCase.listar(bruno.id(), false, e.primeraPagina())
                .contenido().stream().map(NotificacionDto::tipoEvento).toList();
        assertTrue(paraAna.contains(TipoEventoContratacion.SOLICITUD_RECIBIDA));
        assertTrue(paraAna.contains(TipoEventoContratacion.PAGO_CONFIRMADO));
        assertEquals(List.of(TipoEventoContratacion.SOLICITUD_ACEPTADA), paraBruno);
    }

    @Test
    @DisplayName("Caso 3: el pago exacto del saldo disponible tambien es valido (el saldo queda en cero)")
    void caso03_pagarExactamenteElSaldoDisponible() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = e.publicarMentoria(ana.id(), 100, 1, 1);

        ContratacionDto pagada = e.contratarYPagar(ana.id(), bruno.id(), mentoria, 0);

        assertEquals("CONFIRMADA", pagada.estado());
        assertEquals(0, BigDecimal.ZERO.compareTo(e.saldo(bruno.id())));
    }
}
