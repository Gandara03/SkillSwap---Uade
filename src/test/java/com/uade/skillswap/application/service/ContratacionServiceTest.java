package com.uade.skillswap.application.service;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.dto.ComprobanteDto;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.HistorialDto;
import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.exception.RecursoNoEncontradoException;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.domain.exception.CupoAgotadoException;
import com.uade.skillswap.domain.exception.FranjaNoDisponibleException;
import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.exception.SaldoInsuficienteException;
import com.uade.skillswap.domain.exception.TransicionInvalidaException;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.contratacion.observer.ContratacionEvent;
import com.uade.skillswap.domain.model.contratacion.observer.ContratacionObserver;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.model.wallet.TipoMovimiento;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ContratacionService: solicitud, aceptacion, pago (escrow), sesion y consultas")
class ContratacionServiceTest {

    private Escenario e;
    private AplicacionEnMemoria app;
    private PerfilDto ana;      // proveedora
    private PerfilDto bruno;    // cliente
    private PerfilDto cecilia;  // otra clienta
    private PerfilDto dario;    // tercero ajeno a las contrataciones

    @BeforeEach
    void preparar() {
        e = new Escenario();
        app = e.app;
        ana = e.profesional("u-ana", "Ana");
        bruno = e.profesional("u-bruno", "Bruno");
        cecilia = e.profesional("u-cecilia", "Cecilia");
        dario = e.profesional("u-dario", "Dario");
    }

    private List<TipoMovimiento> tiposDeMovimientos(PerfilDto perfil) {
        List<MovimientoDto> movimientos = app.walletUseCase.movimientos(perfil.id(), PaginaRequest.de(0, 50)).contenido();
        List<TipoMovimiento> tipos = new ArrayList<>();
        for (int i = movimientos.size() - 1; i >= 0; i--) { // cronologico: el repo los devuelve del mas nuevo al mas viejo
            tipos.add(movimientos.get(i).tipo());
        }
        return tipos;
    }

    private List<TipoEventoContratacion> eventosDe(PerfilDto perfil) {
        return e.bandeja(perfil).stream().map(NotificacionDto::tipoEvento).toList();
    }

    /** Observer espia: guarda los eventos y si se entregaron dentro o fuera de una transaccion. */
    private static final class Espia implements ContratacionObserver {
        final List<ContratacionEvent> eventos = new ArrayList<>();
        final List<Boolean> dentroDeTransaccion = new ArrayList<>();
        private final AplicacionEnMemoria app;

        Espia(AplicacionEnMemoria app) {
            this.app = app;
        }

        @Override
        public void actualizar(ContratacionEvent evento) {
            eventos.add(evento);
            dentroDeTransaccion.add(app.transaccion.enTransaccion());
        }
    }

    private ContratacionService servicioCon(ContratacionObserver... observers) {
        return new ContratacionService(app.contrataciones, app.publicaciones, app.perfiles, app.wallets,
                app.promociones, List.of(observers), app.transaccion, app.reloj, app.politicas);
    }

    // ---------- solicitar ----------

    @Test
    @DisplayName("solicitar crea una contratacion PENDIENTE con el precio final, sin reservar la franja ni tocar wallets")
    void solicitarCreaPendiente() {
        PublicacionDto publicada = e.publicada(ana);

        ContratacionDto solicitud = e.solicitar(bruno, publicada, 0);

        assertNotNull(solicitud.id());
        assertEquals("PENDIENTE", solicitud.estado());
        assertEquals(bruno.id(), solicitud.clienteId());
        assertEquals(ana.id(), solicitud.proveedorId());
        assertEquals(publicada.id(), solicitud.publicacionId());
        assertEquals("Mentoria de Java", solicitud.publicacionTitulo());
        assertEquals(e.franjaId(publicada, 0), solicitud.franjaId());
        Escenario.assertMonto("40", solicitud.precioFinal());
        assertNull(solicitud.pagadaEn());
        assertEquals(1, solicitud.historial().size());
        assertFalse(e.publicacion(ana, publicada).franjas().get(0).ocupada(), "la franja se reserva recien al aceptar");
        assertEquals(1, e.publicacion(ana, publicada).cupoDisponible());
        Escenario.assertMonto("100", e.saldo(bruno));
    }

    @Test
    @DisplayName("solicitar notifica al proveedor (SOLICITUD_RECIBIDA) y no al cliente")
    void solicitarNotificaAlProveedor() {
        PublicacionDto publicada = e.publicada(ana);

        e.solicitar(bruno, publicada, 0);

        assertEquals(List.of(TipoEventoContratacion.SOLICITUD_RECIBIDA), eventosDe(ana));
        assertTrue(e.bandeja(bruno).isEmpty());
    }

    @Test
    @DisplayName("el precio final incluye las promociones vigentes y queda fijo aunque despues cambien las promociones")
    void precioFinalSeFijaAlSolicitar() {
        PublicacionDto publicada = e.publicada(ana, e.oferta().cupo(2));
        app.publicacionUseCase.agregarPromocion(ana.id(), new CrearPromocionCommand(publicada.id(), "Lanzamiento",
                TipoPromocion.PORCENTAJE, new BigDecimal("25"), null, null));

        ContratacionDto conPromo = e.solicitar(bruno, publicada, 0);
        Escenario.assertMonto("30", conPromo.precioFinal());

        app.publicacionUseCase.agregarPromocion(ana.id(), new CrearPromocionCommand(publicada.id(), "Mas descuento",
                TipoPromocion.MONTO_FIJO, new BigDecimal("10"), null, null));
        ContratacionDto posterior = e.solicitar(cecilia, publicada, 1);

        Escenario.assertMonto("30", e.estado(bruno, conPromo).precioFinal());
        Escenario.assertMonto("20", posterior.precioFinal());
        app.contratacionUseCase.aceptar(ana.id(), conPromo.id());
        app.contratacionUseCase.pagar(bruno.id(), conPromo.id());
        Escenario.assertMonto("70", e.saldo(bruno)); // pago los 30 acordados, no los 20 actuales
    }

    @Test
    @DisplayName("el autor NO puede solicitar su propia publicacion: se rechaza y no se crea ninguna contratacion")
    void solicitarPropiaPublicacion() {
        PublicacionDto publicada = e.publicada(ana);

        assertThrows(ReglaDeNegocioException.class, () -> e.solicitar(ana, publicada, 0));

        assertEquals(0L, app.contratacionUseCase.listarMias(ana.id(), PaginaRequest.de(0, 10)).totalElementos());
        assertTrue(e.bandeja(ana).isEmpty());
    }

    @Test
    @DisplayName("solicitar: publicacion inexistente es RecursoNoEncontrado, perfil inexistente RecursoNoEncontrado, perfil de baja NoAutorizado")
    void solicitarRecursosInexistentes() {
        PublicacionDto publicada = e.publicada(ana);

        assertThrows(RecursoNoEncontradoException.class, () -> app.contratacionUseCase.solicitar(bruno.id(),
                new SolicitarContratacionCommand(999L, 1L, "m", "o")));
        assertThrows(RecursoNoEncontradoException.class, () -> app.contratacionUseCase.solicitar(999L,
                new SolicitarContratacionCommand(publicada.id(), e.franjaId(publicada, 0), "m", "o")));
        app.perfilUseCase.darDeBaja(bruno.id());
        assertThrows(NoAutorizadoException.class, () -> e.solicitar(bruno, publicada, 0));
    }

    @Test
    @DisplayName("solicitar: entradas nulas son ValidacionException; mensaje u objetivo vacios son ReglaDeNegocio")
    void solicitarEntradasInvalidas() {
        PublicacionDto publicada = e.publicada(ana);
        Long franja = e.franjaId(publicada, 0);

        assertThrows(ValidacionException.class, () -> app.contratacionUseCase.solicitar(null,
                new SolicitarContratacionCommand(publicada.id(), franja, "m", "o")));
        assertThrows(ValidacionException.class, () -> app.contratacionUseCase.solicitar(bruno.id(), null));
        assertThrows(ValidacionException.class, () -> app.contratacionUseCase.solicitar(bruno.id(),
                new SolicitarContratacionCommand(null, franja, "m", "o")));
        assertThrows(ValidacionException.class, () -> app.contratacionUseCase.solicitar(bruno.id(),
                new SolicitarContratacionCommand(publicada.id(), null, "m", "o")));
        assertThrows(ReglaDeNegocioException.class, () -> app.contratacionUseCase.solicitar(bruno.id(),
                new SolicitarContratacionCommand(publicada.id(), franja, " ", "o")));
        assertThrows(ReglaDeNegocioException.class, () -> app.contratacionUseCase.solicitar(bruno.id(),
                new SolicitarContratacionCommand(publicada.id(), franja, "m", null)));
        assertEquals(0L, app.contratacionUseCase.listarMias(bruno.id(), PaginaRequest.de(0, 10)).totalElementos());
    }

    @Test
    @DisplayName("solicitar una publicacion que no admite solicitudes (borrador, pausada) es ReglaDeNegocio")
    void solicitarPublicacionNoDisponible() {
        PublicacionDto borrador = e.borrador(ana, e.oferta());
        PublicacionDto pausada = e.publicada(ana);
        app.publicacionUseCase.pausar(ana.id(), pausada.id());

        assertThrows(ReglaDeNegocioException.class, () -> e.solicitar(bruno, borrador, 0));
        assertThrows(ReglaDeNegocioException.class, () -> e.solicitar(bruno, pausada, 0));
    }

    @Test
    @DisplayName("solicitar una franja inexistente, ya ocupada o que ya comenzo es FranjaNoDisponible")
    void solicitarFranjaNoDisponible() {
        PublicacionDto publicada = e.publicada(ana, e.oferta().cupo(2));
        e.aceptada(bruno, ana, publicada, 0);

        assertThrows(FranjaNoDisponibleException.class, () -> app.contratacionUseCase.solicitar(cecilia.id(),
                new SolicitarContratacionCommand(publicada.id(), 99999L, "m", "o")));
        assertThrows(FranjaNoDisponibleException.class, () -> e.solicitar(cecilia, publicada, 0)); // ocupada por Bruno

        PublicacionDto otra = e.publicada(ana, e.oferta().titulo("Otra").franjasEn(10L, 200L));
        e.avanzarHoras(11); // la franja de las 10h ya paso
        assertThrows(FranjaNoDisponibleException.class, () -> e.solicitar(cecilia, otra, 0));
        assertEquals("PENDIENTE", e.solicitar(cecilia, otra, 1).estado(), "la franja futura sigue disponible");
    }

    @Test
    @DisplayName("con el cupo completo ya no se admiten nuevas solicitudes: CupoAgotado")
    void solicitarSinCupo() {
        PublicacionDto publicada = e.publicada(ana); // cupo 1, dos franjas
        e.aceptada(bruno, ana, publicada, 0);

        assertThrows(CupoAgotadoException.class, () -> e.solicitar(cecilia, publicada, 1));
    }

    // ---------- aceptar / rechazar ----------

    @Test
    @DisplayName("aceptar pasa a ACEPTADA, reserva la franja y el cupo en la misma operacion y notifica al cliente")
    void aceptarReservaFranjaYCupo() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto solicitud = e.solicitar(bruno, publicada, 0);

        ContratacionDto aceptada = app.contratacionUseCase.aceptar(ana.id(), solicitud.id());

        assertEquals("ACEPTADA", aceptada.estado());
        PublicacionDto tras = e.publicacion(ana, publicada);
        assertTrue(tras.franjas().get(0).ocupada());
        assertFalse(tras.franjas().get(1).ocupada());
        assertEquals(0, tras.cupoDisponible());
        assertEquals(List.of(TipoEventoContratacion.SOLICITUD_ACEPTADA), eventosDe(bruno));
        Escenario.assertMonto("100", e.saldo(bruno)); // aceptar no cobra
    }

    @Test
    @DisplayName("solo el proveedor acepta: el cliente o un tercero reciben NoAutorizado y nada cambia")
    void aceptarSoloElProveedor() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto solicitud = e.solicitar(bruno, publicada, 0);

        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.aceptar(bruno.id(), solicitud.id()));
        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.aceptar(dario.id(), solicitud.id()));

        assertEquals("PENDIENTE", e.estado(ana, solicitud).estado());
        assertFalse(e.publicacion(ana, publicada).franjas().get(0).ocupada());
        assertTrue(e.bandeja(bruno).isEmpty());
    }

    @Test
    @DisplayName("aceptar una contratacion inexistente es RecursoNoEncontrado y con ids nulos ValidacionException")
    void aceptarInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> app.contratacionUseCase.aceptar(ana.id(), 999L));
        assertThrows(ValidacionException.class, () -> app.contratacionUseCase.aceptar(null, 1L));
        assertThrows(ValidacionException.class, () -> app.contratacionUseCase.aceptar(ana.id(), null));
    }

    @Test
    @DisplayName("aceptar dos veces la misma solicitud es una transicion invalida y no consume otro cupo")
    void aceptarDosVeces() {
        PublicacionDto publicada = e.publicada(ana, e.oferta().cupo(3));
        ContratacionDto aceptada = e.aceptada(bruno, ana, publicada, 0);

        assertThrows(TransicionInvalidaException.class, () -> app.contratacionUseCase.aceptar(ana.id(), aceptada.id()));

        assertEquals(2, e.publicacion(ana, publicada).cupoDisponible());
    }

    @Test
    @DisplayName("CUPO AGOTADO: con cupo 1 se pueden pedir dos franjas distintas, pero aceptar la segunda falla y no reserva nada")
    void aceptarSegundaConCupoUno() {
        PublicacionDto publicada = e.publicada(ana); // cupo 1, franjas 0 y 1
        ContratacionDto deBruno = e.solicitar(bruno, publicada, 0);
        ContratacionDto deCecilia = e.solicitar(cecilia, publicada, 1);
        app.contratacionUseCase.aceptar(ana.id(), deBruno.id());

        assertThrows(CupoAgotadoException.class, () -> app.contratacionUseCase.aceptar(ana.id(), deCecilia.id()));

        PublicacionDto tras = e.publicacion(ana, publicada);
        assertEquals(0, tras.cupoDisponible());
        assertTrue(tras.franjas().get(0).ocupada());
        assertFalse(tras.franjas().get(1).ocupada(), "la franja de la segunda no se reservo");
        assertFalse(eventosDe(cecilia).contains(TipoEventoContratacion.SOLICITUD_ACEPTADA), "no se notifica lo que fallo");
        Escenario.assertMonto("100", e.saldo(cecilia));
    }

    @Test
    @DisplayName("FRANJA OCUPADA: dos clientes piden la misma franja con cupo de sobra; aceptar la segunda falla con FranjaNoDisponible")
    void aceptarFranjaYaOcupada() {
        PublicacionDto publicada = e.publicada(ana, e.oferta().cupo(2));
        ContratacionDto deBruno = e.solicitar(bruno, publicada, 0);
        ContratacionDto deCecilia = e.solicitar(cecilia, publicada, 0);
        app.contratacionUseCase.aceptar(ana.id(), deBruno.id());

        assertThrows(FranjaNoDisponibleException.class, () -> app.contratacionUseCase.aceptar(ana.id(), deCecilia.id()));

        assertEquals(1, e.publicacion(ana, publicada).cupoDisponible(), "solo se consumio el cupo de Bruno");
    }

    @Test
    @DisplayName("rechazar exige motivo, deja la franja libre, notifica al cliente con el motivo y no se puede repetir")
    void rechazarSolicitud() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto solicitud = e.solicitar(bruno, publicada, 0);

        assertThrows(ReglaDeNegocioException.class, () -> app.contratacionUseCase.rechazar(ana.id(), solicitud.id(), " "));
        assertEquals("PENDIENTE", e.estado(ana, solicitud).estado());

        ContratacionDto rechazada = app.contratacionUseCase.rechazar(ana.id(), solicitud.id(), "No tengo disponibilidad");

        assertEquals("RECHAZADA", rechazada.estado());
        HistorialDto ultimo = rechazada.historial().get(rechazada.historial().size() - 1);
        assertEquals("No tengo disponibilidad", ultimo.motivo());
        assertFalse(e.publicacion(ana, publicada).franjas().get(0).ocupada());
        assertEquals(1, e.publicacion(ana, publicada).cupoDisponible());
        NotificacionDto aviso = e.bandeja(bruno).get(0);
        assertEquals(TipoEventoContratacion.SOLICITUD_RECHAZADA, aviso.tipoEvento());
        assertTrue(aviso.mensaje().contains("No tengo disponibilidad"));
        assertThrows(TransicionInvalidaException.class, () -> app.contratacionUseCase.rechazar(ana.id(), solicitud.id(), "otra vez"));
        assertThrows(TransicionInvalidaException.class, () -> app.contratacionUseCase.aceptar(ana.id(), solicitud.id()));
    }

    @Test
    @DisplayName("rechazar: el cliente o un tercero reciben NoAutorizado; una solicitud ya aceptada no se puede rechazar")
    void rechazarPermisosYEstado() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto solicitud = e.solicitar(bruno, publicada, 0);

        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.rechazar(bruno.id(), solicitud.id(), "x"));
        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.rechazar(dario.id(), solicitud.id(), "x"));
        app.contratacionUseCase.aceptar(ana.id(), solicitud.id());
        assertThrows(TransicionInvalidaException.class, () -> app.contratacionUseCase.rechazar(ana.id(), solicitud.id(), "tarde"));
        assertEquals("ACEPTADA", e.estado(ana, solicitud).estado());
    }

    // ---------- pagar (escrow) ----------

    @Test
    @DisplayName("pagar pasa a CONFIRMADA, debita exactamente el precio al cliente, NO acredita aun al proveedor y notifica al proveedor")
    void pagarRetieneElMonto() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto aceptada = e.aceptada(bruno, ana, publicada, 0);

        ContratacionDto pagada = app.contratacionUseCase.pagar(bruno.id(), aceptada.id());

        assertEquals("CONFIRMADA", pagada.estado());
        assertNotNull(pagada.pagadaEn());
        Escenario.assertMonto("60", e.saldo(bruno));
        Escenario.assertMonto("100", e.saldo(ana)); // escrow: el monto queda retenido en la contratacion
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL, TipoMovimiento.PAGO), tiposDeMovimientos(bruno));
        MovimientoDto pago = app.walletUseCase.movimientos(bruno.id(), PaginaRequest.de(0, 10)).contenido().get(0);
        Escenario.assertMonto("40", pago.monto());
        Escenario.assertMonto("60", pago.saldoResultante());
        assertEquals(pagada.id(), pago.contratacionId());
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL), tiposDeMovimientos(ana));
        assertTrue(eventosDe(ana).contains(TipoEventoContratacion.PAGO_CONFIRMADO));
    }

    @Test
    @DisplayName("BUG CORREGIDO (saldo insuficiente): pagar sin saldo falla, la contratacion SIGUE en ACEPTADA y los saldos NO cambian")
    void pagarConSaldoInsuficienteNoCambiaNada() {
        PublicacionDto cara = e.publicada(ana, e.oferta().precio("150")); // Bruno solo tiene 100 de bono
        ContratacionDto aceptada = e.aceptada(bruno, ana, cara, 0);

        SaldoInsuficienteException ex = assertThrows(SaldoInsuficienteException.class,
                () -> app.contratacionUseCase.pagar(bruno.id(), aceptada.id()));

        assertTrue(ex.getMessage().contains("Saldo insuficiente"));
        ContratacionDto tras = e.estado(bruno, aceptada);
        assertEquals("ACEPTADA", tras.estado(), "no debe quedar CONFIRMADA sin haber cobrado");
        assertNull(tras.pagadaEn());
        assertEquals(aceptada.historial().size(), tras.historial().size(), "no se registro ninguna transicion");
        Escenario.assertMonto("100", e.saldo(bruno));
        Escenario.assertMonto("100", e.saldo(ana));
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL), tiposDeMovimientos(bruno));
        assertFalse(eventosDe(ana).contains(TipoEventoContratacion.PAGO_CONFIRMADO), "no se notifica un pago que no ocurrio");
    }

    @Test
    @DisplayName("tras un pago rechazado por saldo, cancelar no reintegra nada (nunca se cobro) y los saldos siguen intactos")
    void pagoFallidoNoDejaDineroRetenido() {
        PublicacionDto cara = e.publicada(ana, e.oferta().precio("150"));
        ContratacionDto aceptada = e.aceptada(bruno, ana, cara, 0);
        assertThrows(SaldoInsuficienteException.class, () -> app.contratacionUseCase.pagar(bruno.id(), aceptada.id()));

        var resultado = app.contratacionUseCase.cancelar(bruno.id(), aceptada.id(), "No me alcanza");

        assertEquals("CANCELADA", resultado.contratacion().estado());
        Escenario.assertMonto("0", resultado.reintegroCliente());
        Escenario.assertMonto("0", resultado.compensacionProveedor());
        Escenario.assertMonto("100", e.saldo(bruno));
        Escenario.assertMonto("100", e.saldo(ana));
    }

    @Test
    @DisplayName("el limite exacto: precio igual al saldo se paga y deja 0; un centavo mas es saldo insuficiente")
    void pagarEnElLimiteDelSaldo() {
        PublicacionDto justa = e.publicada(ana, e.oferta().precio("100"));
        ContratacionDto exacta = e.aceptada(bruno, ana, justa, 0);

        app.contratacionUseCase.pagar(bruno.id(), exacta.id());

        Escenario.assertMonto("0", e.saldo(bruno));
        PublicacionDto pasada = e.publicada(ana, e.oferta().precio("100.01"));
        ContratacionDto de100 = e.aceptada(cecilia, ana, pasada, 0);
        assertThrows(SaldoInsuficienteException.class, () -> app.contratacionUseCase.pagar(cecilia.id(), de100.id()));
        Escenario.assertMonto("100", e.saldo(cecilia));
        assertEquals("ACEPTADA", e.estado(cecilia, de100).estado());
    }

    @Test
    @DisplayName("DOBLE PAGO: el segundo pagar es transicion invalida y el cliente se debita una sola vez")
    void dobloPago() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto pagada = e.pagada(bruno, ana, publicada, 0);

        assertThrows(TransicionInvalidaException.class, () -> app.contratacionUseCase.pagar(bruno.id(), pagada.id()));

        Escenario.assertMonto("60", e.saldo(bruno));
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL, TipoMovimiento.PAGO), tiposDeMovimientos(bruno));
        assertEquals("CONFIRMADA", e.estado(bruno, pagada).estado());
    }

    @Test
    @DisplayName("solo el cliente paga: el proveedor o un tercero reciben NoAutorizado y nadie se debita")
    void pagarSoloElCliente() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto aceptada = e.aceptada(bruno, ana, publicada, 0);

        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.pagar(ana.id(), aceptada.id()));
        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.pagar(dario.id(), aceptada.id()));

        assertEquals("ACEPTADA", e.estado(bruno, aceptada).estado());
        Escenario.assertMonto("100", e.saldo(bruno));
        Escenario.assertMonto("100", e.saldo(ana));
        Escenario.assertMonto("100", e.saldo(dario));
    }

    @Test
    @DisplayName("no se puede pagar una solicitud todavia PENDIENTE ni una inexistente")
    void pagarSinAceptar() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto pendiente = e.solicitar(bruno, publicada, 0);

        assertThrows(TransicionInvalidaException.class, () -> app.contratacionUseCase.pagar(bruno.id(), pendiente.id()));
        assertThrows(RecursoNoEncontradoException.class, () -> app.contratacionUseCase.pagar(bruno.id(), 999L));
        Escenario.assertMonto("100", e.saldo(bruno));
    }

    @Test
    @DisplayName("una promocion del 100 % deja el precio en cero: se paga, se completa y se cancela sin generar movimientos")
    void precioCeroNoGeneraMovimientos() {
        PublicacionDto gratis = e.publicada(ana, e.oferta().cupo(2));
        app.publicacionUseCase.agregarPromocion(ana.id(), new CrearPromocionCommand(gratis.id(), "Gratis",
                TipoPromocion.PORCENTAJE, new BigDecimal("100"), null, null));

        ContratacionDto completada = e.completada(bruno, ana, gratis, 0);
        ContratacionDto cancelable = e.pagada(cecilia, ana, gratis, 1);
        var cancelada = app.contratacionUseCase.cancelar(cecilia.id(), cancelable.id(), "Cambio de planes");

        assertEquals("COMPLETADA", completada.estado());
        Escenario.assertMonto("0", completada.precioFinal());
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL), tiposDeMovimientos(bruno));
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL), tiposDeMovimientos(cecilia));
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL), tiposDeMovimientos(ana));
        Escenario.assertMonto("0", cancelada.reintegroCliente());
    }

    // ---------- iniciar y completar ----------

    @Test
    @DisplayName("iniciar solo lo hace el proveedor y solo sobre una contratacion CONFIRMADA (pagada)")
    void iniciarSesion() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto aceptada = e.aceptada(bruno, ana, publicada, 0);

        assertThrows(TransicionInvalidaException.class, () -> app.contratacionUseCase.iniciar(ana.id(), aceptada.id()));
        app.contratacionUseCase.pagar(bruno.id(), aceptada.id());
        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.iniciar(bruno.id(), aceptada.id()));
        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.iniciar(dario.id(), aceptada.id()));

        assertEquals("EN_CURSO", app.contratacionUseCase.iniciar(ana.id(), aceptada.id()).estado());
        assertThrows(TransicionInvalidaException.class, () -> app.contratacionUseCase.iniciar(ana.id(), aceptada.id()));
    }

    @Test
    @DisplayName("completar acredita al proveedor EXACTAMENTE el precio, una sola vez, y notifica al cliente")
    void completarAcreditaAlProveedor() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto enCurso = e.enCurso(bruno, ana, publicada, 0);
        Escenario.assertMonto("100", e.saldo(ana));

        ContratacionDto completada = app.contratacionUseCase.completar(ana.id(), enCurso.id());

        assertEquals("COMPLETADA", completada.estado());
        assertNotNull(completada.completadaEn());
        Escenario.assertMonto("140", e.saldo(ana));
        Escenario.assertMonto("60", e.saldo(bruno));
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL, TipoMovimiento.COBRO), tiposDeMovimientos(ana));
        MovimientoDto cobro = app.walletUseCase.movimientos(ana.id(), PaginaRequest.de(0, 10)).contenido().get(0);
        Escenario.assertMonto("40", cobro.monto());
        assertEquals(completada.id(), cobro.contratacionId());
        assertEquals(TipoEventoContratacion.SESION_COMPLETADA, e.bandeja(bruno).get(0).tipoEvento());
        // conservacion: lo que salio del cliente es lo que entro al proveedor
        Escenario.assertMonto("200", e.saldo(ana).add(e.saldo(bruno)));
    }

    @Test
    @DisplayName("completar dos veces no cobra dos veces; completar sin iniciar es invalido; el cliente no puede completar")
    void completarCasosNegativos() {
        PublicacionDto publicada = e.publicada(ana, e.oferta().cupo(2));
        ContratacionDto pagada = e.pagada(bruno, ana, publicada, 0);
        assertThrows(TransicionInvalidaException.class, () -> app.contratacionUseCase.completar(ana.id(), pagada.id()));
        app.contratacionUseCase.iniciar(ana.id(), pagada.id());
        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.completar(bruno.id(), pagada.id()));
        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.completar(dario.id(), pagada.id()));
        Escenario.assertMonto("100", e.saldo(ana));

        app.contratacionUseCase.completar(ana.id(), pagada.id());
        assertThrows(TransicionInvalidaException.class, () -> app.contratacionUseCase.completar(ana.id(), pagada.id()));

        Escenario.assertMonto("140", e.saldo(ana));
        assertEquals(List.of(TipoMovimiento.BONO_INICIAL, TipoMovimiento.COBRO), tiposDeMovimientos(ana));
    }

    // ---------- consultas ----------

    @Test
    @DisplayName("obtener solo lo ven los participantes; un tercero NoAutorizado; inexistente RecursoNoEncontrado")
    void obtenerPermisos() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto solicitud = e.solicitar(bruno, publicada, 0);

        assertEquals(solicitud.id(), app.contratacionUseCase.obtener(bruno.id(), solicitud.id()).id());
        assertEquals(solicitud.id(), app.contratacionUseCase.obtener(ana.id(), solicitud.id()).id());
        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.obtener(dario.id(), solicitud.id()));
        assertThrows(RecursoNoEncontradoException.class, () -> app.contratacionUseCase.obtener(ana.id(), 999L));
    }

    @Test
    @DisplayName("el comprobante solo existe para contrataciones COMPLETADAS y solo lo ven sus participantes")
    void comprobante() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto enCurso = e.enCurso(bruno, ana, publicada, 0);

        assertThrows(ReglaDeNegocioException.class, () -> app.contratacionUseCase.comprobante(bruno.id(), enCurso.id()));
        app.contratacionUseCase.completar(ana.id(), enCurso.id());

        ComprobanteDto deCliente = app.contratacionUseCase.comprobante(bruno.id(), enCurso.id());
        ComprobanteDto deProveedor = app.contratacionUseCase.comprobante(ana.id(), enCurso.id());
        assertEquals(deCliente, deProveedor);
        assertEquals(String.format("SS-%08d", enCurso.id()), deCliente.numero());
        assertEquals(enCurso.id(), deCliente.contratacionId());
        assertEquals(publicada.id(), deCliente.publicacionId());
        assertEquals(bruno.id(), deCliente.clienteId());
        assertEquals(ana.id(), deCliente.proveedorId());
        Escenario.assertMonto("40", deCliente.monto());
        assertNotNull(deCliente.pagadoEn());
        assertNotNull(deCliente.completadoEn());
        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.comprobante(dario.id(), enCurso.id()));
        assertThrows(RecursoNoEncontradoException.class, () -> app.contratacionUseCase.comprobante(ana.id(), 999L));
    }

    @Test
    @DisplayName("un tercero NO puede ver el comprobante ni el historial, ni siquiera de una contratacion completada")
    void terceroNoVeComprobanteNiHistorial() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto completada = e.completada(bruno, ana, publicada, 0);

        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.comprobante(dario.id(), completada.id()));
        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.historial(dario.id(), completada.id()));
        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.obtener(dario.id(), completada.id()));
    }

    @Test
    @DisplayName("el historial registra cada transicion con su actor, en orden")
    void historialDelFlujoCompleto() {
        PublicacionDto publicada = e.publicada(ana);
        ContratacionDto completada = e.completada(bruno, ana, publicada, 0);

        List<HistorialDto> historial = app.contratacionUseCase.historial(bruno.id(), completada.id());

        assertEquals(List.of("PENDIENTE", "ACEPTADA", "CONFIRMADA", "EN_CURSO", "COMPLETADA"),
                historial.stream().map(HistorialDto::estadoNuevo).toList());
        assertNull(historial.get(0).estadoAnterior());
        assertEquals("PENDIENTE", historial.get(1).estadoAnterior());
        assertEquals(List.of(bruno.id(), ana.id(), bruno.id(), ana.id(), ana.id()),
                historial.stream().map(HistorialDto::actorId).toList());
        assertEquals(historial, app.contratacionUseCase.historial(ana.id(), completada.id()));
    }

    @Test
    @DisplayName("listarMias muestra las del actor como cliente o proveedor, paginadas y de la mas nueva a la mas vieja")
    void listarMias() {
        PublicacionDto publicada = e.publicada(ana, e.oferta().cupo(2));
        PublicacionDto otra = e.publicada(cecilia, e.oferta().titulo("Taller de SQL"));
        ContratacionDto primera = e.solicitar(bruno, publicada, 0);
        e.avanzarHoras(1);
        ContratacionDto segunda = e.solicitar(bruno, publicada, 1);
        e.avanzarHoras(1);
        ContratacionDto deOtra = e.solicitar(ana, otra, 0); // Ana tambien es clienta de Cecilia

        Pagina<ContratacionDto> deBruno = app.contratacionUseCase.listarMias(bruno.id(), PaginaRequest.de(0, 1));
        Pagina<ContratacionDto> deBrunoPag2 = app.contratacionUseCase.listarMias(bruno.id(), PaginaRequest.de(1, 1));
        Pagina<ContratacionDto> deAna = app.contratacionUseCase.listarMias(ana.id(), PaginaRequest.de(0, 10));

        assertEquals(2L, deBruno.totalElementos());
        assertEquals(segunda.id(), deBruno.contenido().get(0).id());
        assertEquals(primera.id(), deBrunoPag2.contenido().get(0).id());
        assertEquals(List.of(deOtra.id(), segunda.id(), primera.id()), deAna.contenido().stream().map(ContratacionDto::id).toList());
        assertEquals("Taller de SQL", deAna.contenido().get(0).publicacionTitulo());
        assertEquals("Mentoria de Java", deAna.contenido().get(1).publicacionTitulo());
        assertEquals(0L, app.contratacionUseCase.listarMias(dario.id(), PaginaRequest.de(0, 10)).totalElementos());
        assertThrows(ValidacionException.class, () -> app.contratacionUseCase.listarMias(bruno.id(), null));
        assertThrows(ValidacionException.class, () -> app.contratacionUseCase.listarMias(null, PaginaRequest.de(0, 10)));
    }

    // ---------- transacciones y observer ----------

    @Test
    @DisplayName("cada operacion es UNA transaccion y los eventos se entregan DESPUES del commit, con el id definitivo y una sola vez")
    void eventosDespuesDelCommit() {
        Espia espia = new Espia(app);
        ContratacionService servicio = servicioCon(espia);
        PublicacionDto publicada = e.publicada(ana);
        int antes = app.transaccion.ejecuciones();

        ContratacionDto solicitud = servicio.solicitar(bruno.id(), new SolicitarContratacionCommand(publicada.id(),
                e.franjaId(publicada, 0), "Hola", "Aprender"));
        assertEquals(antes + 1, app.transaccion.ejecuciones());
        servicio.aceptar(ana.id(), solicitud.id());
        assertEquals(antes + 2, app.transaccion.ejecuciones());

        assertEquals(List.of(TipoEventoContratacion.SOLICITUD_RECIBIDA, TipoEventoContratacion.SOLICITUD_ACEPTADA),
                espia.eventos.stream().map(ContratacionEvent::getTipo).toList());
        assertTrue(espia.eventos.stream().allMatch(ev -> solicitud.id().equals(ev.getContratacionId())),
                "el evento lleva el id de la contratacion ya guardada");
        assertEquals(List.of(false, false), espia.dentroDeTransaccion, "las notificaciones salen fuera de la transaccion");
    }

    @Test
    @DisplayName("cada observer de la lista que arma el compositor recibe todos los eventos, una sola vez")
    void todosLosObserversDeLaLista() {
        Espia uno = new Espia(app);
        Espia otro = new Espia(app);
        ContratacionService servicio = servicioCon(uno, otro);
        PublicacionDto publicada = e.publicada(ana);

        ContratacionDto solicitud = servicio.solicitar(bruno.id(), new SolicitarContratacionCommand(publicada.id(),
                e.franjaId(publicada, 0), "Hola", "Aprender"));
        servicio.aceptar(ana.id(), solicitud.id());

        List<TipoEventoContratacion> esperados =
                List.of(TipoEventoContratacion.SOLICITUD_RECIBIDA, TipoEventoContratacion.SOLICITUD_ACEPTADA);
        assertEquals(esperados, uno.eventos.stream().map(ContratacionEvent::getTipo).toList());
        assertEquals(esperados, otro.eventos.stream().map(ContratacionEvent::getTipo).toList());
    }

    @Test
    @DisplayName("una operacion que falla no publica eventos")
    void operacionFallidaNoNotifica() {
        Espia espia = new Espia(app);
        ContratacionService servicio = servicioCon(espia);
        PublicacionDto cara = e.publicada(ana, e.oferta().precio("150"));
        ContratacionDto solicitud = servicio.solicitar(bruno.id(), new SolicitarContratacionCommand(cara.id(),
                e.franjaId(cara, 0), "Hola", "Aprender"));
        servicio.aceptar(ana.id(), solicitud.id());
        int eventosAntes = espia.eventos.size();

        assertThrows(SaldoInsuficienteException.class, () -> servicio.pagar(bruno.id(), solicitud.id()));
        assertThrows(NoAutorizadoException.class, () -> servicio.aceptar(dario.id(), solicitud.id()));

        assertEquals(eventosAntes, espia.eventos.size());
    }

    @Test
    @DisplayName("si el notificador falla, la operacion YA confirmada no se deshace ni propaga el error")
    void fallaDelNotificadorNoDeshaceLaOperacion() {
        ContratacionObserver roto = evento -> {
            throw new IllegalStateException("el servicio de notificaciones esta caido");
        };
        ContratacionService servicio = servicioCon(roto);
        PublicacionDto publicada = e.publicada(ana);
        Logger log = Logger.getLogger(ContratacionService.class.getName());
        Level nivel = log.getLevel();
        log.setLevel(Level.OFF); // el servicio registra la falla con WARNING; aca es lo esperado

        ContratacionDto solicitud;
        ContratacionDto aceptada;
        try {
            solicitud = servicio.solicitar(bruno.id(), new SolicitarContratacionCommand(publicada.id(),
                    e.franjaId(publicada, 0), "Hola", "Aprender"));
            aceptada = servicio.aceptar(ana.id(), solicitud.id());
        } finally {
            log.setLevel(nivel);
        }

        assertEquals("ACEPTADA", aceptada.estado());
        assertEquals("ACEPTADA", e.estado(bruno, solicitud).estado());
        assertTrue(e.publicacion(ana, publicada).franjas().get(0).ocupada());
        assertEquals(0, e.publicacion(ana, publicada).cupoDisponible());
    }
}
