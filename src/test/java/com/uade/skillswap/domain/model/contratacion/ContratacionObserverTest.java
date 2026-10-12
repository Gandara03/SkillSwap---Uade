package com.uade.skillswap.domain.model.contratacion;

import com.uade.skillswap.domain.model.contratacion.estado.EstadoAceptada;
import com.uade.skillswap.domain.model.contratacion.estado.EstadoCancelada;
import com.uade.skillswap.domain.model.contratacion.estado.EstadoCompletada;
import com.uade.skillswap.domain.model.contratacion.estado.EstadoConfirmada;
import com.uade.skillswap.domain.model.contratacion.estado.EstadoContratacion;
import com.uade.skillswap.domain.model.contratacion.estado.EstadoContratacionFactory;
import com.uade.skillswap.domain.model.contratacion.estado.EstadoEnCurso;
import com.uade.skillswap.domain.model.contratacion.estado.EstadoPendiente;
import com.uade.skillswap.domain.model.contratacion.estado.EstadoRechazada;
import com.uade.skillswap.domain.model.contratacion.observer.ContratacionEvent;
import com.uade.skillswap.domain.model.contratacion.observer.ContratacionObserver;
import com.uade.skillswap.support.Datos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.uade.skillswap.support.Datos.AHORA;
import static com.uade.skillswap.support.Datos.CLIENTE;
import static com.uade.skillswap.support.Datos.PROVEEDOR;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Contratacion: eventos (Observer) y fabrica de estados")
class ContratacionObserverTest {

    /** Observer de prueba: guarda todo lo que recibe. */
    private static final class Espia implements ContratacionObserver {
        final List<ContratacionEvent> recibidos = new ArrayList<>();

        @Override
        public void actualizar(ContratacionEvent evento) {
            recibidos.add(evento);
        }
    }

    private static List<TipoEventoContratacion> tipos(List<ContratacionEvent> eventos) {
        return eventos.stream().map(ContratacionEvent::getTipo).collect(Collectors.toList());
    }

    @Test
    @DisplayName("el flujo completo acumula un evento por cada hito relevante, en orden")
    void eventosDelFlujo() {
        Contratacion c = Datos.completada();
        assertEquals(List.of(TipoEventoContratacion.SOLICITUD_RECIBIDA, TipoEventoContratacion.SOLICITUD_ACEPTADA,
                TipoEventoContratacion.PAGO_CONFIRMADO, TipoEventoContratacion.SESION_COMPLETADA),
                tipos(c.eventosPendientes()));
    }

    @Test
    @DisplayName("iniciar la sesion no genera evento propio")
    void iniciarSinEvento() {
        Contratacion c = Datos.confirmada();
        int antes = c.eventosPendientes().size();
        c.iniciar(PROVEEDOR, AHORA);
        assertEquals(antes, c.eventosPendientes().size());
    }

    @Test
    @DisplayName("rechazar genera SOLICITUD_RECHAZADA con el motivo como detalle")
    void eventoRechazo() {
        Contratacion c = Datos.pendiente();
        c.rechazar(PROVEEDOR, "no puedo", AHORA);
        ContratacionEvent e = c.eventosPendientes().get(1);
        assertEquals(TipoEventoContratacion.SOLICITUD_RECHAZADA, e.getTipo());
        assertEquals("no puedo", e.getDetalle());
        assertEquals(PROVEEDOR, e.getActorId());
    }

    @Test
    @DisplayName("cancelar genera CANCELACION con el actor y el motivo")
    void eventoCancelacion() {
        Contratacion c = Datos.confirmada();
        c.cancelar(CLIENTE, "viaje", AHORA);
        ContratacionEvent e = c.eventosPendientes().get(c.eventosPendientes().size() - 1);
        assertEquals(TipoEventoContratacion.CANCELACION, e.getTipo());
        assertEquals(CLIENTE, e.getActorId());
        assertEquals("viaje", e.getDetalle());
        assertEquals(CLIENTE, e.getClienteId());
        assertEquals(PROVEEDOR, e.getProveedorId());
    }

    @Test
    @DisplayName("notificarEventosPendientes entrega los eventos a los observers y vacia la lista")
    void notificarVaciaPendientes() {
        Contratacion c = Datos.aceptada();
        Espia espia = new Espia();
        c.agregarObserver(espia);
        c.notificarEventosPendientes();
        assertEquals(List.of(TipoEventoContratacion.SOLICITUD_RECIBIDA, TipoEventoContratacion.SOLICITUD_ACEPTADA),
                tipos(espia.recibidos));
        assertTrue(c.eventosPendientes().isEmpty());
        c.notificarEventosPendientes();
        assertEquals(2, espia.recibidos.size(), "no se re-notifica lo ya notificado");
    }

    @Test
    @DisplayName("los eventos entregados llevan el id definitivo de la contratacion (aunque se crearon antes de guardarla)")
    void eventosConIdDefinitivo() {
        Contratacion c = Contratacion.solicitar(Datos.PUBLICACION_ID, CLIENTE, PROVEEDOR, 1L, "m", "o",
                com.uade.skillswap.domain.model.vo.SkillCoins.de(10), AHORA);
        assertNull(c.eventosPendientes().get(0).getContratacionId());
        Espia espia = new Espia();
        c.agregarObserver(espia);
        Datos.asignarId(c, 77L); // lo que hace el repositorio al guardar
        c.notificarEventosPendientes();
        assertEquals(77L, (long) espia.recibidos.get(0).getContratacionId());
        assertEquals(Datos.PUBLICACION_ID, espia.recibidos.get(0).getPublicacionId());
    }

    @Test
    @DisplayName("todos los observers registrados reciben cada evento")
    void variosObservers() {
        Contratacion c = Datos.pendiente();
        Espia a = new Espia();
        Espia b = new Espia();
        c.agregarObserver(a);
        c.agregarObserver(b);
        c.notificarEventosPendientes();
        assertEquals(1, a.recibidos.size());
        assertEquals(1, b.recibidos.size());
    }

    @Test
    @DisplayName("agregar dos veces el mismo observer no duplica la notificacion; null se ignora")
    void observerDuplicadoONulo() {
        Contratacion c = Datos.pendiente();
        Espia a = new Espia();
        c.agregarObserver(a);
        c.agregarObserver(a);
        c.agregarObserver(null);
        c.notificarEventosPendientes();
        assertEquals(1, a.recibidos.size());
    }

    @Test
    @DisplayName("un observer removido deja de recibir eventos")
    void quitarObserver() {
        Contratacion c = Datos.pendiente();
        Espia a = new Espia();
        c.agregarObserver(a);
        c.quitarObserver(a);
        c.notificarEventosPendientes();
        assertTrue(a.recibidos.isEmpty());
    }

    @Test
    @DisplayName("notificarObservers entrega un evento puntual sin tocar la lista de pendientes")
    void notificarPuntual() {
        Contratacion c = Datos.pendiente();
        Espia a = new Espia();
        c.agregarObserver(a);
        ContratacionEvent e = c.eventosPendientes().get(0);
        c.notificarObservers(e);
        assertEquals(1, a.recibidos.size());
        assertEquals(1, c.eventosPendientes().size());
    }

    // ---------- fabrica de estados ----------

    @Test
    @DisplayName("la fabrica reconstruye cada estado de contratacion desde su nombre")
    void fabrica() {
        assertInstanceOf(EstadoPendiente.class, EstadoContratacionFactory.desdeNombre("PENDIENTE"));
        assertInstanceOf(EstadoAceptada.class, EstadoContratacionFactory.desdeNombre("ACEPTADA"));
        assertInstanceOf(EstadoConfirmada.class, EstadoContratacionFactory.desdeNombre("CONFIRMADA"));
        assertInstanceOf(EstadoEnCurso.class, EstadoContratacionFactory.desdeNombre("EN_CURSO"));
        assertInstanceOf(EstadoCompletada.class, EstadoContratacionFactory.desdeNombre("COMPLETADA"));
        assertInstanceOf(EstadoRechazada.class, EstadoContratacionFactory.desdeNombre("RECHAZADA"));
        assertInstanceOf(EstadoCancelada.class, EstadoContratacionFactory.desdeNombre("CANCELADA"));
        assertInstanceOf(EstadoPendiente.class, EstadoContratacionFactory.desdeNombre(null));
        assertThrows(IllegalArgumentException.class, () -> EstadoContratacionFactory.desdeNombre("???"));
    }

    @Test
    @DisplayName("solo ACEPTADA y CONFIRMADA permiten reprogramar; RECHAZADA, CANCELADA y COMPLETADA son terminales")
    void capacidadesDeEstado() {
        assertTrue(new EstadoAceptada().permiteReprogramacion());
        assertTrue(new EstadoConfirmada().permiteReprogramacion());
        EstadoContratacion[] noReprograman = {new EstadoPendiente(), new EstadoEnCurso(), new EstadoCompletada(),
                new EstadoRechazada(), new EstadoCancelada()};
        for (EstadoContratacion e : noReprograman) {
            assertFalse(e.permiteReprogramacion(), e.getNombre());
        }
        assertTrue(new EstadoRechazada().esTerminal());
        assertTrue(new EstadoCancelada().esTerminal());
        assertTrue(new EstadoCompletada().esTerminal());
        assertFalse(new EstadoEnCurso().esTerminal());
        assertFalse(new EstadoPendiente().esTerminal());
    }
}
