package com.uade.skillswap.application.service;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.dto.CalificacionDto;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.exception.RecursoNoEncontradoException;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("CalificacionService: calificaciones verificadas y reputacion recalculada")
class CalificacionServiceTest {

    private Escenario e;
    private AplicacionEnMemoria app;
    private PerfilDto ana;     // proveedora
    private PerfilDto bruno;   // cliente
    private PerfilDto cecilia; // otra clienta
    private PerfilDto dario;   // tercero
    private PublicacionDto publicada;
    private ContratacionDto completada;

    @BeforeEach
    void preparar() {
        e = new Escenario();
        app = e.app;
        ana = e.profesional("u-ana", "Ana");
        bruno = e.profesional("u-bruno", "Bruno");
        cecilia = e.profesional("u-cecilia", "Cecilia");
        dario = e.profesional("u-dario", "Dario");
        publicada = e.publicada(ana, e.oferta().cupo(3).precio("10").franjasEn(100L, 110L, 120L));
        completada = e.completada(bruno, ana, publicada, 0);
    }

    private CalificacionDto calificar(PerfilDto autor, ContratacionDto contratacion, int puntaje) {
        return app.calificacionUseCase.calificar(autor.id(), new CalificarCommand(contratacion.id(), puntaje, "comentario " + puntaje));
    }

    private PerfilDto perfil(PerfilDto perfil) {
        return app.perfilUseCase.obtener(perfil.id());
    }

    // ---------- caso feliz y reputacion ----------

    @Test
    @DisplayName("el cliente califica al proveedor: la calificacion queda guardada con autor y destinatario correctos")
    void clienteCalificaAlProveedor() {
        CalificacionDto dto = calificar(bruno, completada, 5);

        assertNotNull(dto.id());
        assertEquals(completada.id(), dto.contratacionId());
        assertEquals(bruno.id(), dto.autorId());
        assertEquals(ana.id(), dto.destinatarioId());
        assertEquals(5, dto.puntaje());
        assertEquals("comentario 5", dto.comentario());
        assertNotNull(dto.fecha());
        assertEquals(1L, app.calificacionUseCase.listarRecibidas(ana.id(), PaginaRequest.de(0, 10)).totalElementos());
    }

    @Test
    @DisplayName("calificar recalcula la reputacion del destinatario y no toca la del autor")
    void calificarRecalculaReputacion() {
        calificar(bruno, completada, 4);

        assertEquals(4.0, perfil(ana).reputacion(), 0.0001);
        assertEquals(1, perfil(ana).cantidadCalificaciones());
        assertEquals(0.0, perfil(bruno).reputacion(), 0.0001);
        assertEquals(0, perfil(bruno).cantidadCalificaciones());
    }

    @Test
    @DisplayName("el proveedor tambien puede calificar al cliente, y entonces el destinatario es el cliente")
    void proveedorCalificaAlCliente() {
        CalificacionDto dto = calificar(ana, completada, 3);

        assertEquals(bruno.id(), dto.destinatarioId());
        assertEquals(3.0, perfil(bruno).reputacion(), 0.0001);
        assertEquals(0.0, perfil(ana).reputacion(), 0.0001);
    }

    @Test
    @DisplayName("la reputacion es el promedio de TODAS las calificaciones recibidas, recalculado en cada nueva")
    void reputacionEsPromedio() {
        ContratacionDto segunda = e.completada(cecilia, ana, publicada, 1);
        ContratacionDto tercera = e.completada(dario, ana, publicada, 2);

        calificar(bruno, completada, 5);
        assertEquals(5.0, perfil(ana).reputacion(), 0.0001);
        calificar(cecilia, segunda, 2);
        assertEquals(3.5, perfil(ana).reputacion(), 0.0001);
        calificar(dario, tercera, 3);

        assertEquals(10.0 / 3, perfil(ana).reputacion(), 0.0001);
        assertEquals(3, perfil(ana).cantidadCalificaciones());
    }

    // ---------- negativos ----------

    @Test
    @DisplayName("calificacion UNICA: el mismo autor no califica dos veces la misma contratacion y la reputacion no cambia")
    void calificacionUnica() {
        calificar(bruno, completada, 5);

        assertThrows(ReglaDeNegocioException.class, () -> calificar(bruno, completada, 1));

        assertEquals(1L, app.calificacionUseCase.listarRecibidas(ana.id(), PaginaRequest.de(0, 10)).totalElementos());
        assertEquals(5.0, perfil(ana).reputacion(), 0.0001);
        assertEquals(1, perfil(ana).cantidadCalificaciones());
    }

    @Test
    @DisplayName("cada parte puede calificar una vez: cliente y proveedor se califican mutuamente")
    void calificacionMutua() {
        calificar(bruno, completada, 5);
        calificar(ana, completada, 4);

        assertEquals(5.0, perfil(ana).reputacion(), 0.0001);
        assertEquals(4.0, perfil(bruno).reputacion(), 0.0001);
    }

    @Test
    @DisplayName("solo se califica una contratacion COMPLETADA (no pendiente, aceptada, pagada ni en curso)")
    void soloCompletadas() {
        ContratacionDto pendiente = e.solicitar(cecilia, publicada, 1);
        ContratacionDto enCurso = e.enCurso(dario, ana, publicada, 2);

        assertThrows(ReglaDeNegocioException.class, () -> calificar(cecilia, pendiente, 5));
        assertThrows(ReglaDeNegocioException.class, () -> calificar(dario, enCurso, 5));

        assertEquals(0.0, perfil(ana).reputacion(), 0.0001);
        assertEquals(0L, app.calificacionUseCase.listarRecibidas(ana.id(), PaginaRequest.de(0, 10)).totalElementos());
    }

    @Test
    @DisplayName("un tercero ajeno a la contratacion NO puede calificar")
    void terceroNoCalifica() {
        assertThrows(NoAutorizadoException.class, () -> calificar(dario, completada, 1));

        assertEquals(0.0, perfil(ana).reputacion(), 0.0001);
        assertEquals(0.0, perfil(bruno).reputacion(), 0.0001);
    }

    @Test
    @DisplayName("un puntaje fuera de 1..5 se rechaza y no deja rastro")
    void puntajeInvalido() {
        assertThrows(ReglaDeNegocioException.class, () -> calificar(bruno, completada, 0));
        assertThrows(ReglaDeNegocioException.class, () -> calificar(bruno, completada, 6));
        assertThrows(ReglaDeNegocioException.class, () -> calificar(bruno, completada, -3));

        assertEquals(0L, app.calificacionUseCase.listarRecibidas(ana.id(), PaginaRequest.de(0, 10)).totalElementos());
        // luego de los intentos fallidos puede calificar bien
        calificar(bruno, completada, 1);
        assertEquals(1.0, perfil(ana).reputacion(), 0.0001);
    }

    @Test
    @DisplayName("contratacion inexistente es RecursoNoEncontrado; actor, comando o contratacion nulos son ValidacionException")
    void entradasInvalidas() {
        assertThrows(RecursoNoEncontradoException.class, () -> app.calificacionUseCase.calificar(bruno.id(), new CalificarCommand(999L, 5, "x")));
        assertThrows(ValidacionException.class, () -> app.calificacionUseCase.calificar(null, new CalificarCommand(completada.id(), 5, "x")));
        assertThrows(ValidacionException.class, () -> app.calificacionUseCase.calificar(bruno.id(), null));
        assertThrows(ValidacionException.class, () -> app.calificacionUseCase.calificar(bruno.id(), new CalificarCommand(null, 5, "x")));
    }

    @Test
    @DisplayName("calificar corre en una transaccion")
    void usaTransaccion() {
        int antes = app.transaccion.ejecuciones();

        calificar(bruno, completada, 5);

        assertEquals(antes + 1, app.transaccion.ejecuciones());
    }

    // ---------- listar ----------

    @Test
    @DisplayName("listarRecibidas pagina las calificaciones del perfil, de la mas reciente a la mas antigua")
    void listarRecibidas() {
        ContratacionDto segunda = e.completada(cecilia, ana, publicada, 1);
        ContratacionDto tercera = e.completada(dario, ana, publicada, 2);
        calificar(bruno, completada, 1);
        e.avanzarHoras(1);
        calificar(cecilia, segunda, 3);
        e.avanzarHoras(1);
        calificar(dario, tercera, 5);

        Pagina<CalificacionDto> primera = app.calificacionUseCase.listarRecibidas(ana.id(), PaginaRequest.de(0, 2));
        Pagina<CalificacionDto> segundaPagina = app.calificacionUseCase.listarRecibidas(ana.id(), PaginaRequest.de(1, 2));

        assertEquals(3L, primera.totalElementos());
        assertEquals(5, primera.contenido().get(0).puntaje());
        assertEquals(3, primera.contenido().get(1).puntaje());
        assertEquals(1, segundaPagina.contenido().size());
        assertEquals(1, segundaPagina.contenido().get(0).puntaje());
        assertTrue(primera.contenido().stream().allMatch(c -> c.destinatarioId().equals(ana.id())));
    }

    @Test
    @DisplayName("listarRecibidas: perfil inexistente es RecursoNoEncontrado, pagina nula ValidacionException, sin calificaciones lista vacia")
    void listarRecibidasCasosBorde() {
        assertThrows(RecursoNoEncontradoException.class, () -> app.calificacionUseCase.listarRecibidas(999L, PaginaRequest.de(0, 10)));
        assertThrows(ValidacionException.class, () -> app.calificacionUseCase.listarRecibidas(ana.id(), null));
        assertTrue(app.calificacionUseCase.listarRecibidas(cecilia.id(), PaginaRequest.de(0, 10)).contenido().isEmpty());
    }
}
