package com.uade.skillswap.acceptance.jpa;

import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.wallet.TipoMovimiento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Caso de aceptacion 3 (JPA/H2): solicitar, aceptar y pagar una mentoria con saldo suficiente. */
@DisplayName("Caso 3 (JPA/H2): solicitar, aceptar y pagar una mentoria con saldo suficiente")
class Caso03SolicitarAceptarYPagarJpaTest extends AceptacionJpaBase {

    @Test
    @DisplayName("Caso 3: PENDIENTE -> ACEPTADA -> CONFIRMADA, el cliente paga y todo queda persistido")
    void caso03_solicitarAceptarYPagarConSaldoSuficiente() {
        PerfilDto ana = profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = publicarMentoria("u-ana", 40, 1, 1);
        assertEquals(0, sc(100).compareTo(saldo("u-bruno")), "bono inicial");

        ContratacionDto solicitud = solicitar("u-bruno", mentoria, 0);
        assertEquals("PENDIENTE", solicitud.estado());
        assertEquals(0, sc(40).compareTo(solicitud.precioFinal()));
        assertEquals(0, sc(100).compareTo(saldo("u-bruno")), "solicitar no cobra");

        assertEquals("ACEPTADA", aceptar("u-ana", solicitud.id()).estado());
        PublicacionDto tras = publicacionVista("u-ana", mentoria.id());
        assertEquals(0, tras.cupoDisponible());
        assertTrue(tras.franjas().get(0).ocupada());

        ContratacionDto pagada = pagar("u-bruno", solicitud.id());
        assertEquals("CONFIRMADA", pagada.estado());
        assertNotNull(pagada.pagadaEn());
        assertEquals(0, sc(60).compareTo(saldo("u-bruno")), "100 - 40");
        assertEquals(0, sc(100).compareTo(saldo("u-ana")), "escrow: el proveedor cobra al completar");

        // lo persistido: estado, movimiento PAGO y historial
        assertEquals("CONFIRMADA", contratacionVista("u-bruno", solicitud.id()).estado());
        como("u-bruno");
        MovimientoDto ultimo = ok(app.getWalletController().movimientos(primeraPagina())).contenido().get(0);
        assertEquals(TipoMovimiento.PAGO, ultimo.tipo());
        assertEquals(0, sc(40).compareTo(ultimo.monto()));
        assertEquals(0, sc(60).compareTo(ultimo.saldoResultante()));
        List<String> destinos = ok(app.getContratacionController().historial(solicitud.id())).stream()
                .map(h -> h.estadoNuevo()).toList();
        assertEquals(List.of("PENDIENTE", "ACEPTADA", "CONFIRMADA"), destinos);
        assertEquals(ana.id(), pagada.proveedorId());
    }

    @Test
    @DisplayName("Caso 3: el proveedor es avisado de la solicitud y del pago; el cliente de la aceptacion")
    void caso03_losEventosNotificanALaContraparte() {
        profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = publicarMentoria("u-ana", 40, 1, 1);

        contratarYPagar("u-ana", "u-bruno", mentoria, 0);

        como("u-ana");
        List<TipoEventoContratacion> paraAna = ok(app.getNotificacionController().listar(false, primeraPagina()))
                .contenido().stream().map(NotificacionDto::tipoEvento).toList();
        como("u-bruno");
        List<TipoEventoContratacion> paraBruno = ok(app.getNotificacionController().listar(false, primeraPagina()))
                .contenido().stream().map(NotificacionDto::tipoEvento).toList();
        assertTrue(paraAna.contains(TipoEventoContratacion.SOLICITUD_RECIBIDA));
        assertTrue(paraAna.contains(TipoEventoContratacion.PAGO_CONFIRMADO));
        assertEquals(List.of(TipoEventoContratacion.SOLICITUD_ACEPTADA), paraBruno);
    }
}
