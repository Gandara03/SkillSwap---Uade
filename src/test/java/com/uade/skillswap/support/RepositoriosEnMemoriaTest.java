package com.uade.skillswap.support;

import com.uade.skillswap.domain.model.calificacion.Calificacion;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.promocion.Promocion;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.domain.model.wallet.MovimientoCredito;
import com.uade.skillswap.domain.model.wallet.TipoMovimiento;
import com.uade.skillswap.domain.model.wallet.Wallet;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Set;

import static com.uade.skillswap.support.Datos.AHORA;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica los fakes de soporte (ids, orden, paginacion) y los relojes/transacciones simuladas. */
@DisplayName("Soporte de tests: repositorios en memoria, reloj, transaccion e identidad fakes")
class RepositoriosEnMemoriaTest {

    @Test
    @DisplayName("perfiles: asigna ids secuenciales, busca por id, userId y lote")
    void perfiles() {
        InMemoryPerfilRepository repo = new InMemoryPerfilRepository();
        Perfil a = repo.guardar(Datos.profesional(null, "u-a"));
        Perfil b = repo.guardar(Datos.profesional(null, "u-b"));
        assertEquals(1L, (long) a.getId());
        assertEquals(2L, (long) b.getId());
        assertSame(a, repo.buscarPorUserId("u-a").orElseThrow());
        assertTrue(repo.buscarPorUserId("u-z").isEmpty());
        assertEquals(2, repo.buscarPorIds(Set.of(1L, 2L, 3L)).size());
        repo.guardar(a);
        assertEquals(1L, (long) a.getId(), "guardar de nuevo no cambia el id");
        assertEquals(2, repo.cantidad());
    }

    @Test
    @DisplayName("contrataciones: lista por participante de la mas nueva a la mas vieja y pagina")
    void contrataciones() {
        InMemoryContratacionRepository repo = new InMemoryContratacionRepository();
        for (int i = 0; i < 3; i++) {
            Contratacion c = Contratacion.solicitar(10L, Datos.CLIENTE, Datos.PROVEEDOR, 1L, "m", "o", SkillCoins.de(1),
                    AHORA.plusMinutes(i));
            repo.guardar(c);
        }
        repo.guardar(Contratacion.solicitar(11L, 8L, 9L, 1L, "m", "o", SkillCoins.de(1), AHORA));
        Pagina<Contratacion> p = repo.buscarPorParticipante(Datos.CLIENTE, PaginaRequest.de(0, 2));
        assertEquals(3L, p.totalElementos());
        assertEquals(2, p.contenido().size());
        assertEquals(3L, (long) p.contenido().get(0).getId());
        assertEquals(3L, repo.contarPorPublicacion(10L));
        assertEquals(1L, repo.contarPorPublicacion(11L));
        assertEquals(1L, repo.buscarPorParticipante(Datos.PROVEEDOR, PaginaRequest.de(0, 10)).totalElementos() - 2);
    }

    @Test
    @DisplayName("wallets: persiste movimientos con id, los limpia del agregado y los lista del mas reciente al mas viejo")
    void wallets() {
        InMemoryWalletRepository repo = new InMemoryWalletRepository();
        Wallet w = Wallet.abrir(5L, SkillCoins.de(100), AHORA);
        repo.guardar(w);
        assertNotNull(w.getId());
        assertTrue(w.getMovimientosNuevos().isEmpty());
        w.debitar(SkillCoins.de(10), TipoMovimiento.PAGO, 1L, AHORA.plusMinutes(1));
        repo.guardar(w);
        Pagina<MovimientoCredito> movs = repo.movimientos(5L, PaginaRequest.de(0, 10));
        assertEquals(2L, movs.totalElementos());
        assertEquals(TipoMovimiento.PAGO, movs.contenido().get(0).getTipo());
        assertNotNull(movs.contenido().get(0).getId());
        assertTrue(repo.buscarPorPerfilId(5L).isPresent());
        assertTrue(repo.buscarPorPerfilId(6L).isEmpty());
        assertEquals(0L, repo.movimientos(6L, PaginaRequest.de(0, 10)).totalElementos());
    }

    @Test
    @DisplayName("calificaciones: respeta el unico (contratacion, autor) y lista por destinatario")
    void calificaciones() {
        InMemoryCalificacionRepository repo = new InMemoryCalificacionRepository();
        Contratacion c = Datos.completada();
        Calificacion cal = repo.guardar(Datos.calificacion(c, Datos.CLIENTE, 4));
        assertNotNull(cal.getId());
        assertTrue(repo.existe(c.getId(), Datos.CLIENTE));
        assertFalse(repo.existe(c.getId(), Datos.PROVEEDOR));
        assertThrows(IllegalStateException.class, () -> repo.guardar(Datos.calificacion(c, Datos.CLIENTE, 5)));
        assertEquals(1, repo.buscarPorDestinatario(Datos.PROVEEDOR).size());
        assertEquals(1L, repo.paginarPorDestinatario(Datos.PROVEEDOR, PaginaRequest.de(0, 5)).totalElementos());
        assertTrue(repo.buscarPorDestinatario(Datos.CLIENTE).isEmpty());
    }

    @Test
    @DisplayName("notificaciones: filtra no leidas por destinatario, mas recientes primero")
    void notificaciones() {
        InMemoryNotificacionRepository repo = new InMemoryNotificacionRepository();
        Notificacion vieja = repo.guardar(Notificacion.crear(1L, TipoEventoContratacion.CANCELACION, "a", CanalNotificacion.INTERNA, AHORA));
        Notificacion nueva = repo.guardar(Notificacion.crear(1L, TipoEventoContratacion.CANCELACION, "b", CanalNotificacion.INTERNA, AHORA.plusMinutes(1)));
        repo.guardar(Notificacion.crear(2L, TipoEventoContratacion.CANCELACION, "c", CanalNotificacion.INTERNA, AHORA));
        vieja.marcarLeida();
        repo.guardar(vieja);
        Pagina<Notificacion> todas = repo.buscarPorDestinatario(1L, false, PaginaRequest.de(0, 10));
        assertEquals(2L, todas.totalElementos());
        assertSame(nueva, todas.contenido().get(0));
        assertEquals(1L, repo.buscarPorDestinatario(1L, true, PaginaRequest.de(0, 10)).totalElementos());
        assertEquals(3, repo.todas().size());
        assertTrue(repo.buscarPorId(nueva.getId()).isPresent());
    }

    @Test
    @DisplayName("promociones: devuelve solo las activas de la publicacion")
    void promociones() {
        InMemoryPromocionRepository repo = new InMemoryPromocionRepository();
        Promocion activa = repo.guardar(Promocion.crear(1L, "a", TipoPromocion.PORCENTAJE, BigDecimal.TEN, null, null));
        Promocion apagada = repo.guardar(Promocion.crear(1L, "b", TipoPromocion.PORCENTAJE, BigDecimal.TEN, null, null));
        repo.guardar(Promocion.crear(2L, "c", TipoPromocion.PORCENTAJE, BigDecimal.TEN, null, null));
        apagada.desactivar();
        repo.guardar(apagada);
        assertEquals(List.of(activa), repo.buscarActivasPorPublicacion(1L));
        assertEquals(1, repo.buscarActivasPorPublicacion(2L).size());
    }

    @Test
    @DisplayName("TransaccionSimulada cuenta ejecuciones, devuelve el valor y permite anidar")
    void transaccion() {
        TransaccionSimulada tx = new TransaccionSimulada();
        assertFalse(tx.enTransaccion());
        int valor = tx.ejecutar(() -> {
            assertTrue(tx.enTransaccion());
            tx.ejecutar(() -> { });
            return 42;
        });
        assertEquals(42, valor);
        assertEquals(3 - 1, tx.ejecuciones());
        assertFalse(tx.enTransaccion());
        tx.reiniciarContador();
        assertEquals(0, tx.ejecuciones());
        assertThrows(IllegalStateException.class, () -> tx.ejecutar(() -> { throw new IllegalStateException("x"); }));
        assertFalse(tx.enTransaccion(), "tras una excepcion la transaccion simulada se cierra");
    }

    @Test
    @DisplayName("RelojFijo avanza y se puede fijar")
    void reloj() {
        RelojFijo reloj = new RelojFijo();
        assertEquals(RelojFijo.INICIO_POR_DEFECTO, reloj.ahora());
        reloj.avanzar(Duration.ofHours(5));
        assertEquals(RelojFijo.INICIO_POR_DEFECTO.plusHours(5), reloj.ahora());
        reloj.fijar(AHORA);
        assertEquals(AHORA, reloj.ahora());
    }

    @Test
    @DisplayName("IdentidadFake maneja usuarios y sesion")
    void identidad() {
        IdentidadFake id = new IdentidadFake().agregarUsuario("u-1", "Uno").agregarUsuario("u-2", "Dos", "dos@x", false);
        assertTrue(id.usuarioActual().isEmpty());
        id.iniciarSesion("u-2");
        assertFalse(id.usuarioActual().orElseThrow().activo());
        id.cerrarSesion();
        assertTrue(id.usuarioActual().isEmpty());
        assertTrue(id.buscarPorUserId("u-1").isPresent());
        assertThrows(IllegalArgumentException.class, () -> id.iniciarSesion("u-9"));
    }
}
