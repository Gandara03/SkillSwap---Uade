package com.uade.skillswap.acceptance.jpa;

import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.exception.ErrorCode;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.presentation.Resultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Caso de aceptacion 5 (JPA/H2): rechazar un pago con saldo insuficiente sin modificar saldos ni estado. */
@DisplayName("Caso 5 (JPA/H2): rechazar un pago con saldo insuficiente sin modificar saldos ni estado")
class Caso05SaldoInsuficienteJpaTest extends AceptacionJpaBase {

    @Test
    @DisplayName("Caso 5: pagar 150 SC con 100 SC falla con SALDO_INSUFICIENTE; saldos, estado y movimientos quedan igual")
    void caso05_rechazarPagoConSaldoInsuficienteSinModificarNada() {
        profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        PublicacionDto cara = publicarMentoria("u-ana", 150, 1, 1); // el bono inicial es de 100
        ContratacionDto solicitud = solicitar("u-bruno", cara, 0);
        aceptar("u-ana", solicitud.id());
        como("u-bruno");
        long movimientosAntes = ok(app.getWalletController().movimientos(primeraPagina())).totalElementos();
        long historialAntes = ok(app.getContratacionController().historial(solicitud.id(), primeraPagina())).totalElementos();

        Resultado<ContratacionDto> intento = app.getContratacionController().pagar(solicitud.id());

        assertTrue(intento.esError());
        assertEquals(ErrorCode.SALDO_INSUFICIENTE, intento.codigo());
        // saldos intactos
        assertEquals(0, sc(100).compareTo(saldo("u-bruno")));
        assertEquals(0, sc(100).compareTo(saldo("u-ana")));
        // estado persistido igual, sin fecha de pago ni transiciones nuevas
        ContratacionDto tras = contratacionVista("u-bruno", solicitud.id());
        assertEquals("ACEPTADA", tras.estado());
        assertNull(tras.pagadaEn());
        assertEquals(historialAntes, ok(app.getContratacionController().historial(solicitud.id(), primeraPagina())).totalElementos());
        assertEquals(movimientosAntes, ok(app.getWalletController().movimientos(primeraPagina())).totalElementos());
        // el proveedor no recibio el aviso de pago confirmado
        como("u-ana");
        assertFalse(ok(app.getNotificacionController().listar(false, primeraPagina())).contenido().stream()
                .map(NotificacionDto::tipoEvento).anyMatch(t -> t == TipoEventoContratacion.PAGO_CONFIRMADO));
    }
}
