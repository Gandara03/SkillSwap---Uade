package com.uade.skillswap.domain.model.wallet;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.exception.SaldoInsuficienteException;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.uade.skillswap.support.Datos.AHORA;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Wallet")
class WalletTest {

    @Test
    @DisplayName("abrir con bono inicial acredita el saldo y registra el movimiento BONO_INICIAL")
    void abrirConBono() {
        Wallet w = Wallet.abrir(1L, SkillCoins.de(100), AHORA);
        assertEquals(SkillCoins.de(100), w.getSaldo());
        assertEquals(1, w.getMovimientosNuevos().size());
        MovimientoCredito m = w.getMovimientosNuevos().get(0);
        assertEquals(TipoMovimiento.BONO_INICIAL, m.getTipo());
        assertEquals(SkillCoins.de(100), m.getMonto());
        assertEquals(SkillCoins.de(100), m.getSaldoPosterior());
        assertEquals(1L, (long) m.getPerfilId());
        assertNull(m.getContratacionId());
    }

    @Test
    @DisplayName("abrir sin bono (nulo o cero) deja saldo cero y sin movimientos")
    void abrirSinBono() {
        assertEquals(SkillCoins.CERO, Wallet.abrir(1L, null, AHORA).getSaldo());
        Wallet w = Wallet.abrir(1L, SkillCoins.CERO, AHORA);
        assertEquals(SkillCoins.CERO, w.getSaldo());
        assertTrue(w.getMovimientosNuevos().isEmpty());
    }

    @Test
    @DisplayName("debitar descuenta el saldo y registra el movimiento con el saldo posterior")
    void debitar() {
        Wallet w = Wallet.abrir(1L, SkillCoins.de(100), AHORA);
        w.debitar(SkillCoins.de(30), TipoMovimiento.PAGO, 9L, AHORA);
        assertEquals(SkillCoins.de(70), w.getSaldo());
        MovimientoCredito m = w.getMovimientosNuevos().get(1);
        assertEquals(TipoMovimiento.PAGO, m.getTipo());
        assertEquals(SkillCoins.de(30), m.getMonto());
        assertEquals(SkillCoins.de(70), m.getSaldoPosterior());
        assertEquals(9L, (long) m.getContratacionId());
    }

    @Test
    @DisplayName("se puede debitar todo el saldo exacto (queda en cero)")
    void debitarExacto() {
        Wallet w = Wallet.abrir(1L, SkillCoins.de(50), AHORA);
        w.debitar(SkillCoins.de(50), TipoMovimiento.PAGO, 1L, AHORA);
        assertEquals(SkillCoins.CERO, w.getSaldo());
    }

    @Test
    @DisplayName("saldo insuficiente lanza excepcion y NO modifica saldo ni movimientos")
    void saldoInsuficienteNoModificaNada() {
        Wallet w = Wallet.abrir(1L, SkillCoins.de(10), AHORA);
        SaldoInsuficienteException e = assertThrows(SaldoInsuficienteException.class,
                () -> w.debitar(SkillCoins.de("10.01"), TipoMovimiento.PAGO, 1L, AHORA));
        assertNotNull(e.getMessage());
        assertEquals(SkillCoins.de(10), w.getSaldo());
        assertEquals(1, w.getMovimientosNuevos().size(), "solo el bono inicial");
    }

    @Test
    @DisplayName("acreditar suma al saldo con COBRO o REINTEGRO")
    void acreditar() {
        Wallet w = Wallet.abrir(1L, SkillCoins.de(10), AHORA);
        w.acreditar(SkillCoins.de(40), TipoMovimiento.COBRO, 5L, AHORA);
        w.acreditar(SkillCoins.de("5.50"), TipoMovimiento.REINTEGRO, 5L, AHORA);
        assertEquals(SkillCoins.de("55.50"), w.getSaldo());
        assertEquals(3, w.getMovimientosNuevos().size());
        assertTrue(w.getMovimientosNuevos().get(2).getTipo().esCredito());
    }

    @Test
    @DisplayName("el monto debe ser positivo: cero, negativo o nulo se rechazan sin tocar el saldo")
    void montoInvalido() {
        Wallet w = Wallet.abrir(1L, SkillCoins.de(10), AHORA);
        assertThrows(ReglaDeNegocioException.class, () -> w.acreditar(SkillCoins.CERO, TipoMovimiento.COBRO, 1L, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> w.acreditar(SkillCoins.de(-5), TipoMovimiento.COBRO, 1L, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> w.acreditar(null, TipoMovimiento.COBRO, 1L, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> w.debitar(SkillCoins.CERO, TipoMovimiento.PAGO, 1L, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> w.debitar(SkillCoins.de(-5), TipoMovimiento.PAGO, 1L, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> w.debitar(null, TipoMovimiento.PAGO, 1L, AHORA));
        assertEquals(SkillCoins.de(10), w.getSaldo());
        assertEquals(1, w.getMovimientosNuevos().size());
    }

    @Test
    @DisplayName("el tipo de movimiento debe coincidir con la operacion (no se debita con tipo credito ni al reves)")
    void tipoIncoherente() {
        Wallet w = Wallet.abrir(1L, SkillCoins.de(10), AHORA);
        assertThrows(ReglaDeNegocioException.class, () -> w.debitar(SkillCoins.de(1), TipoMovimiento.COBRO, 1L, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> w.debitar(SkillCoins.de(1), TipoMovimiento.REINTEGRO, 1L, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> w.acreditar(SkillCoins.de(1), TipoMovimiento.PAGO, 1L, AHORA));
        assertEquals(SkillCoins.de(10), w.getSaldo());
    }

    @Test
    @DisplayName("los tipos credito son BONO_INICIAL, COBRO y REINTEGRO; PAGO es debito")
    void tiposDeMovimiento() {
        assertTrue(TipoMovimiento.BONO_INICIAL.esCredito());
        assertTrue(TipoMovimiento.COBRO.esCredito());
        assertTrue(TipoMovimiento.REINTEGRO.esCredito());
        assertFalse(TipoMovimiento.PAGO.esCredito());
    }

    @Test
    @DisplayName("limpiarMovimientosNuevos vacia la lista pero conserva el saldo")
    void limpiarMovimientos() {
        Wallet w = Wallet.abrir(1L, SkillCoins.de(10), AHORA);
        w.limpiarMovimientosNuevos();
        assertTrue(w.getMovimientosNuevos().isEmpty());
        assertEquals(SkillCoins.de(10), w.getSaldo());
    }

    @Test
    @DisplayName("la lista de movimientos nuevos es de solo lectura")
    void movimientosInmutables() {
        Wallet w = Wallet.abrir(1L, SkillCoins.de(10), AHORA);
        assertThrows(UnsupportedOperationException.class, () -> w.getMovimientosNuevos().clear());
    }

    @Test
    @DisplayName("reconstruir rehidrata saldo y version sin movimientos nuevos")
    void reconstruir() {
        Wallet w = Wallet.reconstruir(4L, 1L, SkillCoins.de(25), 6L);
        assertEquals(4L, (long) w.getId());
        assertEquals(SkillCoins.de(25), w.getSaldo());
        assertEquals(6L, (long) w.getVersion());
        assertTrue(w.getMovimientosNuevos().isEmpty());
    }

    @Test
    @DisplayName("la secuencia de operaciones mantiene la coherencia de saldos posteriores")
    void secuencia() {
        Wallet w = Wallet.abrir(1L, SkillCoins.de(100), AHORA);
        w.debitar(SkillCoins.de(40), TipoMovimiento.PAGO, 1L, AHORA);
        w.acreditar(SkillCoins.de(10), TipoMovimiento.REINTEGRO, 1L, AHORA);
        w.debitar(SkillCoins.de(20), TipoMovimiento.PAGO, 2L, AHORA);
        assertEquals(SkillCoins.de(50), w.getSaldo());
        assertEquals(SkillCoins.de(60), w.getMovimientosNuevos().get(1).getSaldoPosterior());
        assertEquals(SkillCoins.de(70), w.getMovimientosNuevos().get(2).getSaldoPosterior());
        assertEquals(SkillCoins.de(50), w.getMovimientosNuevos().get(3).getSaldoPosterior());
    }
}
