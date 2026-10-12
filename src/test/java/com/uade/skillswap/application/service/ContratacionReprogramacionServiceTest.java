package com.uade.skillswap.application.service;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.exception.RecursoNoEncontradoException;
import com.uade.skillswap.domain.exception.FranjaNoDisponibleException;
import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.exception.TransicionInvalidaException;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ContratacionService: reprogramacion de la reserva (propuesta, aceptacion y rechazo)")
class ContratacionReprogramacionServiceTest {

    private Escenario e;
    private AplicacionEnMemoria app;
    private PerfilDto ana;     // proveedora
    private PerfilDto bruno;   // cliente
    private PerfilDto cecilia; // otra clienta
    private PerfilDto dario;   // tercero
    private PublicacionDto publicada; // cupo 2, franjas a 100 h, 130 h y 160 h
    private ContratacionDto aceptada; // de Bruno, sobre la franja 0

    @BeforeEach
    void preparar() {
        e = new Escenario();
        app = e.app;
        ana = e.profesional("u-ana", "Ana");
        bruno = e.profesional("u-bruno", "Bruno");
        cecilia = e.profesional("u-cecilia", "Cecilia");
        dario = e.profesional("u-dario", "Dario");
        publicada = e.publicada(ana, e.oferta().cupo(2).franjasEn(100L, 130L, 160L));
        aceptada = e.aceptada(bruno, ana, publicada, 0);
    }

    private Long franja(int indice) {
        return e.franjaId(publicada, indice);
    }

    private List<TipoEventoContratacion> eventosDe(PerfilDto perfil) {
        return e.bandeja(perfil).stream().map(NotificacionDto::tipoEvento).toList();
    }

    // ---------- proponer ----------

    @Test
    @DisplayName("el cliente propone otra franja libre: queda la propuesta pendiente, la reserva NO se mueve y se avisa al proveedor")
    void clienteProponeFranja() {
        ContratacionDto conPropuesta = app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), franja(1));

        assertNotNull(conPropuesta.propuestaReprogramacion());
        assertEquals(franja(1), conPropuesta.propuestaReprogramacion().nuevaFranjaId());
        assertEquals(bruno.id(), conPropuesta.propuestaReprogramacion().propuestaPorId());
        assertEquals(franja(0), conPropuesta.franjaId(), "hasta que no se acepte la franja es la original");
        PublicacionDto tras = e.publicacion(ana, publicada);
        assertTrue(tras.franjas().get(0).ocupada());
        assertFalse(tras.franjas().get(1).ocupada());
        assertTrue(eventosDe(ana).contains(TipoEventoContratacion.REPROGRAMACION_PROPUESTA));
        assertFalse(eventosDe(bruno).contains(TipoEventoContratacion.REPROGRAMACION_PROPUESTA));
    }

    @Test
    @DisplayName("el proveedor tambien puede proponer y entonces se avisa al cliente")
    void proveedorProponeFranja() {
        ContratacionDto conPropuesta = app.contratacionUseCase.proponerReprogramacion(ana.id(), aceptada.id(), franja(1));

        assertEquals(ana.id(), conPropuesta.propuestaReprogramacion().propuestaPorId());
        assertTrue(eventosDe(bruno).contains(TipoEventoContratacion.REPROGRAMACION_PROPUESTA));
    }

    @Test
    @DisplayName("un tercero NO puede proponer una reprogramacion")
    void terceroNoPropone() {
        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.proponerReprogramacion(dario.id(), aceptada.id(), franja(1)));

        assertNull(e.estado(bruno, aceptada).propuestaReprogramacion());
    }

    @Test
    @DisplayName("proponer la franja que ya tiene la reserva es ReglaDeNegocio")
    void proponerLaFranjaActual() {
        assertThrows(ReglaDeNegocioException.class, () -> app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), franja(0)));

        assertNull(e.estado(bruno, aceptada).propuestaReprogramacion());
    }

    @Test
    @DisplayName("proponer una franja inexistente es FranjaNoDisponible")
    void proponerFranjaInexistente() {
        assertThrows(FranjaNoDisponibleException.class, () -> app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), 99999L));
    }

    @Test
    @DisplayName("proponer una franja ocupada por otra reserva es FranjaNoDisponible")
    void proponerFranjaOcupada() {
        e.aceptada(cecilia, ana, publicada, 2); // Cecilia ocupa la franja 2

        assertThrows(FranjaNoDisponibleException.class, () -> app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), franja(2)));
    }

    @Test
    @DisplayName("no se puede proponer una franja que ya comenzo")
    void proponerFranjaPasada() {
        PublicacionDto conPasada = e.publicada(ana, e.oferta().titulo("Con franja cercana").cupo(2).franjasEn(200L, 5L));
        ContratacionDto contratacion = e.aceptada(cecilia, ana, conPasada, 0);
        e.avanzarHoras(6); // la franja 1 (a las 5 h) ya paso

        assertThrows(FranjaNoDisponibleException.class, () -> app.contratacionUseCase.proponerReprogramacion(cecilia.id(), contratacion.id(), e.franjaId(conPasada, 1)));
    }

    @Test
    @DisplayName("no se puede reprogramar una solicitud PENDIENTE ni una cancelada ni una en curso")
    void estadosQueNoPermitenReprogramar() {
        ContratacionDto pendiente = e.solicitar(cecilia, publicada, 1);
        ContratacionDto cancelada = e.aceptada(dario, ana, publicada, 2);
        app.contratacionUseCase.cancelar(dario.id(), cancelada.id(), "Ya no");

        assertThrows(TransicionInvalidaException.class, () -> app.contratacionUseCase.proponerReprogramacion(cecilia.id(), pendiente.id(), franja(2)));
        assertThrows(TransicionInvalidaException.class, () -> app.contratacionUseCase.proponerReprogramacion(dario.id(), cancelada.id(), franja(1)));
        app.contratacionUseCase.pagar(bruno.id(), aceptada.id());
        app.contratacionUseCase.iniciar(ana.id(), aceptada.id());
        assertThrows(TransicionInvalidaException.class, () -> app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), franja(1)));
    }

    @Test
    @DisplayName("mientras hay una propuesta pendiente no se admite otra")
    void propuestaDuplicada() {
        app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), franja(1));

        assertThrows(ReglaDeNegocioException.class, () -> app.contratacionUseCase.proponerReprogramacion(ana.id(), aceptada.id(), franja(2)));

        assertEquals(franja(1), e.estado(bruno, aceptada).propuestaReprogramacion().nuevaFranjaId());
    }

    @Test
    @DisplayName("reprogramar una contratacion inexistente es RecursoNoEncontrado")
    void proponerInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> app.contratacionUseCase.proponerReprogramacion(bruno.id(), 999L, franja(1)));
        assertThrows(RecursoNoEncontradoException.class, () -> app.contratacionUseCase.responderReprogramacion(ana.id(), 999L, true));
    }

    // ---------- responder ----------

    @Test
    @DisplayName("al ACEPTAR, la reserva se mueve: franja vieja libre, franja nueva ocupada, el cupo no cambia y se avisa al proponente")
    void aceptarMueveLaReserva() {
        app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), franja(1));
        int cupoAntes = e.publicacion(ana, publicada).cupoDisponible();

        ContratacionDto movida = app.contratacionUseCase.responderReprogramacion(ana.id(), aceptada.id(), true);

        assertEquals(franja(1), movida.franjaId());
        assertNull(movida.propuestaReprogramacion());
        PublicacionDto tras = e.publicacion(ana, publicada);
        assertFalse(tras.franjas().get(0).ocupada(), "la franja anterior queda libre");
        assertTrue(tras.franjas().get(1).ocupada(), "la nueva queda reservada");
        assertEquals(cupoAntes, tras.cupoDisponible());
        assertTrue(eventosDe(bruno).contains(TipoEventoContratacion.REPROGRAMACION_ACEPTADA));
        assertEquals("ACEPTADA", movida.estado(), "reprogramar no cambia el estado de la contratacion");
    }

    @Test
    @DisplayName("al RECHAZAR, todo queda como estaba: misma franja, reserva intacta y propuesta descartada")
    void rechazarMantieneLaReserva() {
        app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), franja(1));

        ContratacionDto igual = app.contratacionUseCase.responderReprogramacion(ana.id(), aceptada.id(), false);

        assertEquals(franja(0), igual.franjaId());
        assertNull(igual.propuestaReprogramacion());
        PublicacionDto tras = e.publicacion(ana, publicada);
        assertTrue(tras.franjas().get(0).ocupada());
        assertFalse(tras.franjas().get(1).ocupada());
        assertTrue(eventosDe(bruno).contains(TipoEventoContratacion.REPROGRAMACION_RECHAZADA));
        // tras el rechazo se puede volver a proponer
        assertNotNull(app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), franja(2)).propuestaReprogramacion());
    }

    @Test
    @DisplayName("el PROPONENTE no puede responder su propia propuesta (NoAutorizado) y la propuesta sigue pendiente")
    void proponenteNoResponde() {
        app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), franja(1));

        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.responderReprogramacion(bruno.id(), aceptada.id(), true));
        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.responderReprogramacion(bruno.id(), aceptada.id(), false));

        ContratacionDto tras = e.estado(ana, aceptada);
        assertEquals(franja(0), tras.franjaId());
        assertNotNull(tras.propuestaReprogramacion());
        assertTrue(e.publicacion(ana, publicada).franjas().get(0).ocupada());
    }

    @Test
    @DisplayName("si propuso el proveedor, quien responde es el cliente")
    void respondeElCliente() {
        app.contratacionUseCase.proponerReprogramacion(ana.id(), aceptada.id(), franja(1));

        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.responderReprogramacion(ana.id(), aceptada.id(), true));
        ContratacionDto movida = app.contratacionUseCase.responderReprogramacion(bruno.id(), aceptada.id(), true);

        assertEquals(franja(1), movida.franjaId());
        assertTrue(eventosDe(ana).contains(TipoEventoContratacion.REPROGRAMACION_ACEPTADA));
    }

    @Test
    @DisplayName("un tercero NO puede responder una propuesta")
    void terceroNoResponde() {
        app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), franja(1));

        assertThrows(NoAutorizadoException.class, () -> app.contratacionUseCase.responderReprogramacion(dario.id(), aceptada.id(), true));

        assertEquals(franja(0), e.estado(ana, aceptada).franjaId());
    }

    @Test
    @DisplayName("responder sin propuesta pendiente es ReglaDeNegocio")
    void responderSinPropuesta() {
        assertThrows(ReglaDeNegocioException.class, () -> app.contratacionUseCase.responderReprogramacion(ana.id(), aceptada.id(), true));
    }

    @Test
    @DisplayName("si la franja propuesta se ocupo antes de aceptar, aceptar falla con FranjaNoDisponible y la reserva original sigue")
    void franjaOcupadaEntreMedio() {
        app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), franja(1));
        e.aceptada(cecilia, ana, publicada, 1); // mientras tanto Cecilia toma la franja 1

        assertThrows(FranjaNoDisponibleException.class, () -> app.contratacionUseCase.responderReprogramacion(ana.id(), aceptada.id(), true));

        PublicacionDto tras = e.publicacion(ana, publicada);
        assertTrue(tras.franjas().get(0).ocupada(), "la reserva de Bruno sigue en su franja original");
        assertTrue(tras.franjas().get(1).ocupada(), "la franja 1 sigue siendo de Cecilia");
        assertEquals(0, tras.cupoDisponible());
    }

    // ---------- con pago y cancelacion ----------

    @Test
    @DisplayName("una contratacion CONFIRMADA (ya pagada) se reprograma sin mover dinero")
    void reprogramarContratacionPagada() {
        app.contratacionUseCase.pagar(bruno.id(), aceptada.id());

        app.contratacionUseCase.proponerReprogramacion(ana.id(), aceptada.id(), franja(2));
        ContratacionDto movida = app.contratacionUseCase.responderReprogramacion(bruno.id(), aceptada.id(), true);

        assertEquals("CONFIRMADA", movida.estado());
        assertEquals(franja(2), movida.franjaId());
        Escenario.assertMonto("60", e.saldo(bruno));
        Escenario.assertMonto("100", e.saldo(ana));
    }

    @Test
    @DisplayName("tras reprogramar, cancelar libera la franja NUEVA y la original queda libre")
    void cancelarTrasReprogramar() {
        app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), franja(1));
        app.contratacionUseCase.responderReprogramacion(ana.id(), aceptada.id(), true);

        app.contratacionUseCase.cancelar(bruno.id(), aceptada.id(), "Ya no necesito la sesion");

        PublicacionDto tras = e.publicacion(ana, publicada);
        assertTrue(tras.franjas().stream().noneMatch(f -> f.ocupada()));
        assertEquals(2, tras.cupoDisponible());
    }

    @Test
    @DisplayName("el historial deja constancia de la propuesta y de su respuesta")
    void historialDeLaReprogramacion() {
        app.contratacionUseCase.proponerReprogramacion(bruno.id(), aceptada.id(), franja(1));
        app.contratacionUseCase.responderReprogramacion(ana.id(), aceptada.id(), true);

        var historial = app.contratacionUseCase.historial(bruno.id(), aceptada.id());

        List<String> motivos = historial.stream().map(h -> h.motivo()).toList();
        assertTrue(motivos.contains("Reprogramacion propuesta"));
        assertTrue(motivos.contains("Reprogramacion aceptada"));
    }
}
