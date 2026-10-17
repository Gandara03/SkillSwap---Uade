package com.uade.skillswap.support;

import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.FranjaCommand;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.PaginaRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Humo del compositor en memoria: cablea los servicios reales con los fakes y recorre el flujo principal.
 * No reemplaza los tests de servicios: solo prueba que el soporte funciona de punta a punta.
 */
@DisplayName("AplicacionEnMemoria: flujo principal de punta a punta con fakes")
class AplicacionEnMemoriaSmokeTest {

    private PerfilDto registrar(AplicacionEnMemoria app, String userId, String nombre, TipoPerfil tipo, CanalNotificacion canal) {
        app.identidad.agregarUsuario(userId, nombre);
        app.identidad.iniciarSesion(userId);
        return app.perfilUseCase.registrar(new RegistrarPerfilCommand(userId, nombre, tipo, "UTC", null,
                Set.of("java"), Set.of("java"), List.of(), canal));
    }

    @Test
    @DisplayName("publicar -> solicitar -> aceptar -> pagar -> iniciar -> completar -> calificar, con wallets y notificaciones")
    void flujoCompleto() {
        AplicacionEnMemoria app = AplicacionEnMemoria.crear();
        PerfilDto ana = registrar(app, "u-ana", "Ana", TipoPerfil.PROFESIONAL, CanalNotificacion.EMAIL);
        PerfilDto bruno = registrar(app, "u-bruno", "Bruno", TipoPerfil.PROFESIONAL, CanalNotificacion.INTERNA);

        assertEquals(0, new BigDecimal("100").compareTo(app.walletUseCase.consultar(ana.id()).saldo()), "bono inicial");

        // Ana publica una mentoria
        app.identidad.iniciarSesion("u-ana");
        PublicacionDto borrador = app.publicacionUseCase.crearBorrador(ana.id(), new CrearPublicacionCommand(
                TipoPublicacion.MENTORIA, "Mentoria Java", "desc", "programacion", Modalidad.REMOTA, 60, 1,
                new BigDecimal("40"), Set.of("java"), Set.of("java"), TipoPoliticaCancelacion.FLEXIBLE,
                List.of(new FranjaCommand(app.reloj.ahora().plusDays(3), app.reloj.ahora().plusDays(3).plusHours(1)))));
        PublicacionDto publicada = app.publicacionUseCase.publicar(ana.id(), borrador.id());
        assertEquals("PUBLICADA", publicada.estado());

        // Bruno la encuentra y la solicita
        assertEquals(1L, app.busquedaUseCase.buscar(FiltroBusqueda.vacio(), PaginaRequest.de(0, 10)).totalElementos());
        Long franjaId = publicada.franjas().get(0).id();
        ContratacionDto solicitud = app.contratacionUseCase.solicitar(bruno.id(),
                new SolicitarContratacionCommand(publicada.id(), franjaId, "Hola", "Aprender"));
        assertEquals("PENDIENTE", solicitud.estado());

        ContratacionDto aceptada = app.contratacionUseCase.aceptar(ana.id(), solicitud.id());
        assertEquals("ACEPTADA", aceptada.estado());
        ContratacionDto pagada = app.contratacionUseCase.pagar(bruno.id(), solicitud.id());
        assertEquals("CONFIRMADA", pagada.estado());
        assertEquals(0, new BigDecimal("60").compareTo(app.walletUseCase.consultar(bruno.id()).saldo()));
        app.contratacionUseCase.iniciar(ana.id(), solicitud.id());
        ContratacionDto completada = app.contratacionUseCase.completar(ana.id(), solicitud.id());
        assertEquals("COMPLETADA", completada.estado());
        assertEquals(0, new BigDecimal("140").compareTo(app.walletUseCase.consultar(ana.id()).saldo()));

        app.calificacionUseCase.calificar(bruno.id(), new CalificarCommand(solicitud.id(), 5, "muy bien"));
        assertEquals(5.0, app.perfilUseCase.obtener(ana.id()).reputacion(), 0.0001);

        // Observer + canales: Ana (EMAIL) recibio notificacion interna y por correo; hubo transacciones
        assertFalse(app.notificaciones.todas().isEmpty());
        assertFalse(app.emailSdk.enviados().isEmpty());
        assertTrue(app.pushSdk.enviados().isEmpty());
        assertTrue(app.transaccion.ejecuciones() > 0);
    }

    @Test
    @DisplayName("un proveedor con canal PUSH recibe la notificacion interna y ademas el push (dos tipos ante un mismo evento)")
    void dosNotificacionesPorEvento() {
        AplicacionEnMemoria app = AplicacionEnMemoria.crear();
        PerfilDto ana = registrar(app, "u-ana", "Ana", TipoPerfil.PROFESIONAL, CanalNotificacion.PUSH);
        PerfilDto bruno = registrar(app, "u-bruno", "Bruno", TipoPerfil.PROFESIONAL, CanalNotificacion.INTERNA);
        PublicacionDto pub = app.publicacionUseCase.crearBorrador(ana.id(), new CrearPublicacionCommand(
                TipoPublicacion.MENTORIA, "T", "d", "c", Modalidad.REMOTA, 60, 1, BigDecimal.TEN, Set.of(), Set.of(),
                null, List.of(new FranjaCommand(app.reloj.ahora().plusDays(2), app.reloj.ahora().plusDays(2).plusHours(1)))));
        app.publicacionUseCase.publicar(ana.id(), pub.id());
        Long franja = app.publicacionUseCase.obtener(ana.id(), pub.id()).franjas().get(0).id();
        app.contratacionUseCase.solicitar(bruno.id(), new SolicitarContratacionCommand(pub.id(), franja, "m", "o"));
        // el proveedor (Ana, PUSH) recibe SOLICITUD_RECIBIDA por la bandeja interna y por push
        assertEquals(1, app.pushSdk.enviados().size());
        assertEquals(2L, app.notificaciones.buscarPorDestinatario(ana.id(), false, PaginaRequest.de(0, 10)).totalElementos());
    }
}
