package com.uade.skillswap.infrastructure.persistence.jpa;

import com.uade.skillswap.application.exception.ConflictoConcurrenciaException;
import com.uade.skillswap.domain.exception.SaldoInsuficienteException;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.domain.model.wallet.MovimientoCredito;
import com.uade.skillswap.domain.model.wallet.TipoMovimiento;
import com.uade.skillswap.domain.model.wallet.Wallet;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaWalletRepositoryTest extends JpaTestBase {

    private Perfil ana;

    @BeforeEach
    void crearPerfil() {
        ana = profesional("u-ana");
    }

    private Wallet abrir(Perfil perfil, long saldoInicial) {
        return wallets.guardar(Wallet.abrir(perfil.getId(), SkillCoins.de(saldoInicial), AHORA));
    }

    private Wallet recargar() {
        return wallets.buscarPorPerfilId(ana.getId()).orElseThrow();
    }

    private List<MovimientoCredito> movimientos() {
        return wallets.movimientos(ana.getId(), PaginaRequest.de(0, 100)).contenido();
    }

    @Test
    @DisplayName("abrir una wallet con bono persiste el saldo y el movimiento, y limpia los movimientos del agregado")
    void abrirConBono() {
        Wallet nueva = Wallet.abrir(ana.getId(), SkillCoins.de(500), AHORA);
        assertEquals(1, nueva.getMovimientosNuevos().size());

        Wallet guardada = wallets.guardar(nueva);

        assertSame(nueva, guardada);
        assertNotNull(guardada.getId());
        assertEquals(Long.valueOf(0), guardada.getVersion());
        assertTrue(guardada.getMovimientosNuevos().isEmpty(), "los movimientos persistidos se limpian del agregado");
        Wallet leida = recargar();
        assertEquals(SkillCoins.de(500), leida.getSaldo());
        assertEquals(ana.getId(), leida.getPerfilId());
        assertEquals(1, movimientos().size());
        MovimientoCredito bono = movimientos().get(0);
        assertEquals(TipoMovimiento.BONO_INICIAL, bono.getTipo());
        assertEquals(SkillCoins.de(500), bono.getMonto());
        assertEquals(SkillCoins.de(500), bono.getSaldoPosterior());
        assertNull(bono.getContratacionId());
        assertEquals(AHORA, bono.getFecha());
        assertEquals(ana.getId(), bono.getPerfilId());
        assertNotNull(bono.getId());
    }

    @Test
    @DisplayName("una wallet sin bono se abre en cero y sin movimientos")
    void abrirSinBono() {
        wallets.guardar(Wallet.abrir(ana.getId(), SkillCoins.CERO, AHORA));

        assertEquals(SkillCoins.CERO, recargar().getSaldo());
        assertTrue(movimientos().isEmpty());
    }

    @Test
    @DisplayName("debitar y acreditar persisten saldo y movimientos; el historial va del mas reciente al mas antiguo")
    void debitarYAcreditar() {
        abrir(ana, 500);
        Perfil bruno = profesional("u-bruno");
        Publicacion publicacion = publicacion(bruno).guardar();
        Contratacion contratacion = solicitud(publicacion, ana);

        Wallet w = recargar();
        w.debitar(SkillCoins.de(120), TipoMovimiento.PAGO, contratacion.getId(), AHORA.plusMinutes(1));
        wallets.guardar(w);
        w = recargar();
        w.acreditar(SkillCoins.de(30), TipoMovimiento.REINTEGRO, contratacion.getId(), AHORA.plusMinutes(2));
        wallets.guardar(w);

        Wallet leida = recargar();
        assertEquals(SkillCoins.de(410), leida.getSaldo());
        assertEquals(Long.valueOf(2), leida.getVersion());
        List<MovimientoCredito> historial = movimientos();
        assertIterableEquals(List.of(TipoMovimiento.REINTEGRO, TipoMovimiento.PAGO, TipoMovimiento.BONO_INICIAL),
                historial.stream().map(MovimientoCredito::getTipo).toList());
        assertIterableEquals(List.of(SkillCoins.de(410), SkillCoins.de(380), SkillCoins.de(500)),
                historial.stream().map(MovimientoCredito::getSaldoPosterior).toList());
        assertEquals(contratacion.getId(), historial.get(0).getContratacionId());
    }

    @Test
    @DisplayName("los movimientos se paginan con total y orden estable")
    void movimientosPaginados() {
        Wallet w = abrir(ana, 100);
        for (int i = 1; i <= 4; i++) {
            w.acreditar(SkillCoins.de(10), TipoMovimiento.COBRO, null, AHORA.plusMinutes(i));
        }
        wallets.guardar(w);

        Pagina<MovimientoCredito> primera = wallets.movimientos(ana.getId(), PaginaRequest.de(0, 2));
        Pagina<MovimientoCredito> tercera = wallets.movimientos(ana.getId(), PaginaRequest.de(2, 2));

        assertEquals(5, primera.totalElementos());
        assertEquals(3, primera.totalPaginas());
        assertEquals(SkillCoins.de(140), primera.contenido().get(0).getSaldoPosterior());
        assertEquals(SkillCoins.de(130), primera.contenido().get(1).getSaldoPosterior());
        assertEquals(1, tercera.contenido().size());
        assertEquals(TipoMovimiento.BONO_INICIAL, tercera.contenido().get(0).getTipo());
        assertEquals(0, wallets.movimientos(9999L, PaginaRequest.de(0, 5)).totalElementos());
    }

    @Test
    @DisplayName("un debito con saldo insuficiente falla en el dominio y no modifica lo persistido")
    void saldoInsuficienteNoPersisteNada() {
        abrir(ana, 100);

        Wallet w = recargar();
        assertThrows(SaldoInsuficienteException.class,
                () -> w.debitar(SkillCoins.de(200), TipoMovimiento.PAGO, null, AHORA));
        wallets.guardar(w);

        assertEquals(SkillCoins.de(100), recargar().getSaldo());
        assertEquals(1, movimientos().size());
    }

    @Test
    @DisplayName("hay una sola wallet por perfil (restriccion unica)")
    void unaWalletPorPerfil() {
        abrir(ana, 100);

        assertThrows(PersistenceException.class, () -> abrir(ana, 50));
        assertEquals(1, wallets.listarEntidades().size());
    }

    @Test
    @DisplayName("buscarPorPerfilId devuelve vacio si el perfil no tiene wallet")
    void buscarSinWallet() {
        assertTrue(wallets.buscarPorPerfilId(ana.getId()).isEmpty());
        assertTrue(wallets.buscarPorPerfilId(null).isEmpty());
    }

    @Test
    @DisplayName("rollback: si un movimiento viola una FK, tampoco queda la wallet ni el saldo")
    void rollbackSiFallaUnMovimiento() {
        Wallet w = Wallet.abrir(ana.getId(), SkillCoins.de(100), AHORA);
        w.debitar(SkillCoins.de(10), TipoMovimiento.PAGO, 9999L, AHORA);

        assertThrows(PersistenceException.class, () -> wallets.guardar(w));

        assertTrue(wallets.buscarPorPerfilId(ana.getId()).isEmpty());
        assertTrue(wallets.listarEntidades().isEmpty());
    }

    @Test
    @DisplayName("optimistic locking: dos lecturas de la misma wallet, el segundo guardado falla y no pisa al primero")
    void conflictoDeVersion() {
        abrir(ana, 500);
        Wallet lectorA = recargar();
        Wallet lectorB = recargar();
        lectorA.debitar(SkillCoins.de(100), TipoMovimiento.PAGO, null, AHORA.plusMinutes(1));
        lectorB.debitar(SkillCoins.de(200), TipoMovimiento.PAGO, null, AHORA.plusMinutes(1));

        wallets.guardar(lectorA);
        assertThrows(ConflictoConcurrenciaException.class, () -> wallets.guardar(lectorB));

        assertEquals(SkillCoins.de(400), recargar().getSaldo());
        assertEquals(2, movimientos().size(), "el movimiento del guardado fallido no quedo registrado");
    }

    @Test
    @DisplayName("optimistic locking real: dos unidades de trabajo concurrentes sobre la misma wallet")
    void conflictoEntreDosUnidadesDeTrabajo() throws Exception {
        abrir(ana, 500);
        CountDownLatch leido = new CountDownLatch(1);
        CountDownLatch continuar = new CountDownLatch(1);
        AtomicReference<Throwable> falloDelLento = new AtomicReference<>();

        Thread lento = new Thread(() -> {
            try {
                transaccion.ejecutar(() -> {
                    Wallet w = recargar();
                    leido.countDown();
                    esperar(continuar);
                    w.debitar(SkillCoins.de(100), TipoMovimiento.PAGO, null, AHORA.plusMinutes(1));
                    wallets.guardar(w);
                });
            } catch (Throwable t) {
                falloDelLento.set(t);
            }
        });
        lento.start();
        assertTrue(leido.await(10, TimeUnit.SECONDS), "el hilo lento no llego a leer la wallet");

        // mientras el otro hilo mantiene su unidad de trabajo abierta con la version vieja, esta confirma un cambio
        transaccion.ejecutar(() -> {
            Wallet w = recargar();
            w.debitar(SkillCoins.de(50), TipoMovimiento.PAGO, null, AHORA.plusMinutes(2));
            wallets.guardar(w);
        });
        continuar.countDown();
        lento.join(10_000);

        assertTrue(falloDelLento.get() instanceof ConflictoConcurrenciaException,
                "esperaba ConflictoConcurrenciaException y fue " + falloDelLento.get());
        assertEquals(SkillCoins.de(450), recargar().getSaldo());
        assertEquals(2, movimientos().size());
    }

    private static void esperar(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timeout esperando al otro hilo");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
