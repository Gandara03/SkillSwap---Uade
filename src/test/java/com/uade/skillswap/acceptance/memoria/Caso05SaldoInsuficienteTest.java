package com.uade.skillswap.acceptance.memoria;

import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.domain.exception.SaldoInsuficienteException;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Caso de aceptacion 5: rechazar un pago con saldo insuficiente sin modificar saldos ni estado. */
@DisplayName("Caso 5 (memoria): rechazar un pago con saldo insuficiente sin modificar saldos ni estado")
class Caso05SaldoInsuficienteTest {

    private static BigDecimal sc(long monto) {
        return BigDecimal.valueOf(monto);
    }

    @Test
    @DisplayName("Caso 5: pagar 150 SC con 100 SC falla; saldos, estado y movimientos quedan igual")
    void caso05_rechazarPagoConSaldoInsuficienteSinModificarNada() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto cara = e.publicarMentoria(ana.id(), 150, 1, 1); // el bono inicial es de 100
        ContratacionDto solicitud = e.solicitar(bruno.id(), cara, 0);
        e.app.contratacionUseCase.aceptar(ana.id(), solicitud.id());
        long movimientosAntes = e.app.walletUseCase.movimientos(bruno.id(), e.primeraPagina()).totalElementos();
        int historialAntes = e.app.contratacionUseCase.historial(bruno.id(), solicitud.id()).size();

        SaldoInsuficienteException error = assertThrows(SaldoInsuficienteException.class,
                () -> e.app.contratacionUseCase.pagar(bruno.id(), solicitud.id()));
        assertTrue(error.getMessage().contains("insuficiente"), error.getMessage());

        // saldos intactos
        assertEquals(0, sc(100).compareTo(e.saldo(bruno.id())));
        assertEquals(0, sc(100).compareTo(e.saldo(ana.id())));
        // la contratacion sigue ACEPTADA, sin fecha de pago, y no se registro ninguna transicion nueva
        ContratacionDto tras = e.app.contratacionUseCase.obtener(bruno.id(), solicitud.id());
        assertEquals("ACEPTADA", tras.estado());
        assertNull(tras.pagadaEn());
        assertEquals(historialAntes, e.app.contratacionUseCase.historial(bruno.id(), solicitud.id()).size());
        // no hay movimientos nuevos en la wallet del cliente
        assertEquals(movimientosAntes, e.app.walletUseCase.movimientos(bruno.id(), e.primeraPagina()).totalElementos());
        // y el proveedor no recibio el aviso de pago confirmado
        assertFalse(e.app.notificacionUseCase.listar(ana.id(), false, e.primeraPagina()).contenido().stream()
                .map(NotificacionDto::tipoEvento).anyMatch(t -> t == TipoEventoContratacion.PAGO_CONFIRMADO));
    }

    @Test
    @DisplayName("Caso 5: reintentar el pago mismo cliente y mismo precio vuelve a fallar sin efectos")
    void caso05_elReintentoFallaDeLaMismaManera() {
        EscenarioMemoria e = new EscenarioMemoria();
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto cara = e.publicarMentoria(ana.id(), 101, 1, 1); // un SkillCoin mas de lo que tiene
        ContratacionDto solicitud = e.solicitar(bruno.id(), cara, 0);
        e.app.contratacionUseCase.aceptar(ana.id(), solicitud.id());

        assertThrows(SaldoInsuficienteException.class, () -> e.app.contratacionUseCase.pagar(bruno.id(), solicitud.id()));
        assertThrows(SaldoInsuficienteException.class, () -> e.app.contratacionUseCase.pagar(bruno.id(), solicitud.id()));

        assertEquals(0, sc(100).compareTo(e.saldo(bruno.id())));
        assertEquals("ACEPTADA", e.app.contratacionUseCase.obtener(bruno.id(), solicitud.id()).estado());
    }
}
