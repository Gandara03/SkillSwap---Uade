package com.uade.skillswap.application.service;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.command.ActualizarPerfilCommand;
import com.uade.skillswap.application.command.BloqueDisponibilidadDto;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PerfilPublicoDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.exception.RecursoNoEncontradoException;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.application.politica.ConfiguracionCancelacion;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.domain.model.wallet.TipoMovimiento;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PerfilService: registro, edicion, baja logica y perfil publico")
class PerfilServiceTest {

    private Escenario e;
    private AplicacionEnMemoria app;

    @BeforeEach
    void preparar() {
        e = new Escenario();
        app = e.app;
    }

    private RegistrarPerfilCommand comandoDe(String userId, String nombre) {
        return new RegistrarPerfilCommand(userId, nombre, TipoPerfil.PROFESIONAL, "UTC", "bio",
                Set.of("Java"), Set.of("Spring"), List.of(), CanalNotificacion.PUSH);
    }

    // ---------- registrar ----------

    @Test
    @DisplayName("registrar crea el perfil con sus datos y una wallet con el bono inicial de 100 SC")
    void registrarCreaPerfilYWallet() {
        app.identidad.agregarUsuario("u-ana", "Ana");
        app.identidad.iniciarSesion("u-ana");

        PerfilDto perfil = app.perfilUseCase.registrar(new RegistrarPerfilCommand("u-ana", "  Ana Perez ",
                TipoPerfil.PROFESIONAL, "America/Argentina/Buenos_Aires", "Mentora", Set.of("Java", "SQL"),
                Set.of("DDD"), List.of(new BloqueDisponibilidadDto(DayOfWeek.MONDAY, LocalTime.of(9, 0),
                LocalTime.of(12, 0))), CanalNotificacion.EMAIL));

        assertNotNull(perfil.id());
        assertEquals("u-ana", perfil.userId());
        assertEquals("Ana Perez", perfil.nombreVisible());
        assertEquals(TipoPerfil.PROFESIONAL, perfil.tipo());
        assertEquals("America/Argentina/Buenos_Aires", perfil.zonaHoraria());
        assertEquals(Set.of("java", "sql"), perfil.habilidades());
        assertEquals(CanalNotificacion.EMAIL, perfil.canalPreferido());
        assertEquals(1, perfil.disponibilidad().size());
        assertTrue(perfil.activo());
        assertEquals(0.0, perfil.reputacion(), 0.0001);
        assertEquals(0, perfil.cantidadCalificaciones());

        Escenario.assertMonto("100", e.saldo(perfil));
        List<MovimientoDto> movimientos = app.walletUseCase.movimientos(perfil.id(), PaginaRequest.de(0, 10)).contenido();
        assertEquals(1, movimientos.size());
        assertEquals(TipoMovimiento.BONO_INICIAL, movimientos.get(0).tipo());
        Escenario.assertMonto("100", movimientos.get(0).monto());
    }

    @Test
    @DisplayName("el bono inicial sale de la configuracion y no esta hardcodeado")
    void bonoInicialConfigurable() {
        SkillSwapConfig config = new SkillSwapConfig(SkillCoins.de(250), 200, 10, 5, 5, ConfiguracionCancelacion.porDefecto());
        Escenario otro = new Escenario(AplicacionEnMemoria.crear(config));

        PerfilDto ana = otro.profesional("u-ana", "Ana");

        Escenario.assertMonto("250", otro.saldo(ana));
    }

    @Test
    @DisplayName("registrar con un userId distinto al de la sesion es NoAutorizado y no crea nada")
    void registrarUserIdAjeno() {
        app.identidad.agregarUsuario("u-ana", "Ana").agregarUsuario("u-bruno", "Bruno");
        app.identidad.iniciarSesion("u-ana");

        assertThrows(NoAutorizadoException.class, () -> app.perfilUseCase.registrar(comandoDe("u-bruno", "Bruno")));
        assertEquals(0, app.perfiles.cantidad());
    }

    @Test
    @DisplayName("registrar sin sesion iniciada es NoAutorizado")
    void registrarSinSesion() {
        app.identidad.agregarUsuario("u-ana", "Ana");
        app.identidad.cerrarSesion();

        assertThrows(NoAutorizadoException.class, () -> app.perfilUseCase.registrar(comandoDe("u-ana", "Ana")));
    }

    @Test
    @DisplayName("un usuario inactivo en identidad no puede registrar perfil")
    void registrarUsuarioInactivo() {
        app.identidad.agregarUsuario("u-ana", "Ana", "ana@test.local", false);
        app.identidad.iniciarSesion("u-ana");

        assertThrows(NoAutorizadoException.class, () -> app.perfilUseCase.registrar(comandoDe("u-ana", "Ana")));
        assertEquals(0, app.perfiles.cantidad());
    }

    @Test
    @DisplayName("un segundo registro del mismo usuario falla con ValidacionException y no duplica wallet ni bono")
    void registrarDuplicado() {
        PerfilDto ana = e.profesional("u-ana", "Ana");

        assertThrows(ValidacionException.class, () -> app.perfilUseCase.registrar(comandoDe("u-ana", "Ana de nuevo")));

        assertEquals(1, app.perfiles.cantidad());
        Escenario.assertMonto("100", e.saldo(ana));
    }

    @Test
    @DisplayName("entradas invalidas: comando nulo, userId vacio, zona horaria inexistente, nombre vacio")
    void registrarEntradasInvalidas() {
        app.identidad.agregarUsuario("u-ana", "Ana");
        app.identidad.iniciarSesion("u-ana");

        assertThrows(ValidacionException.class, () -> app.perfilUseCase.registrar(null));
        assertThrows(ValidacionException.class, () -> app.perfilUseCase.registrar(comandoDe(" ", "Ana")));
        assertThrows(ValidacionException.class, () -> app.perfilUseCase.registrar(new RegistrarPerfilCommand("u-ana",
                "Ana", TipoPerfil.PROFESIONAL, "Marte/Olympus", null, Set.of(), Set.of(), List.of(), null)));
        assertThrows(ReglaDeNegocioException.class, () -> app.perfilUseCase.registrar(comandoDe("u-ana", "  ")));
        assertEquals(0, app.perfiles.cantidad());
    }

    // ---------- actualizar y obtener ----------

    @Test
    @DisplayName("actualizar cambia los datos editables y el canal preferido, pero no la reputacion ni el userId")
    void actualizarPerfil() {
        PerfilDto ana = e.profesional("u-ana", "Ana");

        PerfilDto nuevo = app.perfilUseCase.actualizar(ana.id(), new ActualizarPerfilCommand("Ana P.", "Nueva bio",
                "Europe/Madrid", Set.of("Kotlin"), Set.of("Cloud"), List.of(), CanalNotificacion.EMAIL));

        assertEquals("Ana P.", nuevo.nombreVisible());
        assertEquals("Nueva bio", nuevo.biografia());
        assertEquals("Europe/Madrid", nuevo.zonaHoraria());
        assertEquals(Set.of("kotlin"), nuevo.habilidades());
        assertEquals(Set.of("Cloud"), nuevo.intereses());
        assertEquals(CanalNotificacion.EMAIL, nuevo.canalPreferido());
        assertEquals("u-ana", nuevo.userId());
        assertEquals(nuevo, app.perfilUseCase.obtener(ana.id()));
    }

    @Test
    @DisplayName("actualizar o consultar un perfil inexistente es RecursoNoEncontrado")
    void perfilInexistente() {
        e.profesional("u-ana", "Ana");

        assertThrows(RecursoNoEncontradoException.class, () -> app.perfilUseCase.obtener(999L));
        assertThrows(RecursoNoEncontradoException.class, () -> app.perfilUseCase.actualizar(999L,
                new ActualizarPerfilCommand("X", null, null, null, null, null, null)));
        assertThrows(RecursoNoEncontradoException.class, () -> app.perfilUseCase.verPerfilPublico(999L));
        assertThrows(RecursoNoEncontradoException.class, () -> app.perfilUseCase.darDeBaja(999L));
    }

    @Test
    @DisplayName("entradas nulas se rechazan con ValidacionException")
    void entradasNulas() {
        PerfilDto ana = e.profesional("u-ana", "Ana");

        assertThrows(ValidacionException.class, () -> app.perfilUseCase.actualizar(null,
                new ActualizarPerfilCommand("X", null, null, null, null, null, null)));
        assertThrows(ValidacionException.class, () -> app.perfilUseCase.actualizar(ana.id(), null));
        assertThrows(ValidacionException.class, () -> app.perfilUseCase.obtener(null));
        assertThrows(ValidacionException.class, () -> app.perfilUseCase.darDeBaja(null));
    }

    // ---------- baja logica ----------

    @Test
    @DisplayName("darDeBaja es logica: el perfil queda inactivo pero conserva wallet e historial")
    void bajaLogica() {
        PerfilDto ana = e.profesional("u-ana", "Ana");

        app.perfilUseCase.darDeBaja(ana.id());

        PerfilDto tras = app.perfilUseCase.obtener(ana.id());
        assertFalse(tras.activo());
        assertEquals("Ana", tras.nombreVisible());
        Escenario.assertMonto("100", e.saldo(ana));
        assertEquals(1, app.walletUseCase.movimientos(ana.id(), PaginaRequest.de(0, 10)).totalElementos());
    }

    @Test
    @DisplayName("un perfil de baja no puede editarse, crear publicaciones ni solicitar contrataciones")
    void perfilDeBajaNoOpera() {
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto publicada = e.publicada(ana);
        app.perfilUseCase.darDeBaja(bruno.id());

        assertThrows(ReglaDeNegocioException.class, () -> app.perfilUseCase.actualizar(bruno.id(),
                new ActualizarPerfilCommand("Otro", null, null, null, null, null, null)));
        assertThrows(NoAutorizadoException.class,
                () -> app.publicacionUseCase.crearBorrador(bruno.id(), e.comando(e.oferta())));
        assertThrows(NoAutorizadoException.class, () -> e.solicitar(bruno, publicada, 0));
    }

    @Test
    @DisplayName("la baja pausa las publicaciones publicadas del perfil: deja de recibir solicitudes y de aparecer en busquedas")
    void bajaPausaPublicaciones() {
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto publicada = e.publicada(ana);
        PublicacionDto borrador = e.borrador(ana, e.oferta().titulo("Borrador"));

        app.perfilUseCase.darDeBaja(ana.id());

        assertEquals("PAUSADA", e.publicacion(ana, publicada).estado());
        assertEquals("BORRADOR", e.publicacion(ana, borrador).estado(), "un borrador no se toca");
        assertThrows(ReglaDeNegocioException.class, () -> e.solicitar(bruno, publicada, 0));
        assertEquals(0L, app.busquedaUseCase.buscar(null, PaginaRequest.de(0, 10)).totalElementos());
    }

    @Test
    @DisplayName("la baja conserva las contrataciones existentes: el cliente puede seguir consultandolas")
    void bajaConservaHistorial() {
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        ContratacionDto pagada = e.pagada(bruno, ana, e.publicada(ana), 0);

        app.perfilUseCase.darDeBaja(ana.id());

        assertEquals("CONFIRMADA", e.estado(bruno, pagada).estado());
        assertEquals(1L, app.contratacionUseCase.listarMias(ana.id(), PaginaRequest.de(0, 10)).totalElementos());
    }

    // ---------- perfil publico (Facade) ----------

    @Test
    @DisplayName("el perfil publico arma reputacion, ultimas calificaciones y solo las publicaciones activas")
    void perfilPublico() {
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");
        PublicacionDto publicada = e.publicada(ana, e.oferta().titulo("Activa").cupo(2));
        e.borrador(ana, e.oferta().titulo("Solo borrador"));
        PublicacionDto pausada = e.publicada(ana, e.oferta().titulo("Pausada"));
        app.publicacionUseCase.pausar(ana.id(), pausada.id());
        ContratacionDto completada = e.completada(bruno, ana, publicada, 0);
        e.calificar(bruno, completada, 4);

        PerfilPublicoDto publico = app.perfilUseCase.verPerfilPublico(ana.id());

        assertEquals(ana.id(), publico.id());
        assertEquals("Ana", publico.nombreVisible());
        assertEquals(Set.of("java"), publico.habilidades());
        assertEquals(4.0, publico.reputacion(), 0.0001);
        assertEquals(1, publico.cantidadCalificaciones());
        assertEquals(1, publico.ultimasCalificaciones().size());
        assertEquals(4, publico.ultimasCalificaciones().get(0).puntaje());
        assertEquals(1, publico.publicacionesActivas().size(), "solo la PUBLICADA");
        assertEquals("Activa", publico.publicacionesActivas().get(0).titulo());
        assertEquals("Ana", publico.publicacionesActivas().get(0).autorNombre());
        assertEquals(4.0, publico.publicacionesActivas().get(0).autorReputacion(), 0.0001);
    }

    @Test
    @DisplayName("el perfil publico limita calificaciones y publicaciones segun la configuracion y lista primero las mas recientes")
    void perfilPublicoRespetaLimites() {
        SkillSwapConfig config = new SkillSwapConfig(SkillCoins.de(100), 200, 10, 2, 1, ConfiguracionCancelacion.porDefecto());
        Escenario otro = new Escenario(AplicacionEnMemoria.crear(config));
        PerfilDto ana = otro.profesional("u-ana", "Ana");
        PerfilDto bruno = otro.profesional("u-bruno", "Bruno");
        PublicacionDto principal = otro.publicada(ana, otro.oferta().cupo(3).precio("10").franjasEn(100L, 110L, 120L));
        otro.publicada(ana, otro.oferta().titulo("Segunda publicada"));
        int[] puntajes = {1, 3, 5};
        for (int i = 0; i < puntajes.length; i++) {
            ContratacionDto c = otro.completada(bruno, ana, principal, i);
            otro.avanzarHoras(1); // cada calificacion en un instante distinto
            otro.calificar(bruno, c, puntajes[i]);
        }

        PerfilPublicoDto publico = otro.app.perfilUseCase.verPerfilPublico(ana.id());

        assertEquals(3, publico.cantidadCalificaciones());
        assertEquals(3.0, publico.reputacion(), 0.0001);
        assertEquals(2, publico.ultimasCalificaciones().size(), "tope configurado");
        assertEquals(5, publico.ultimasCalificaciones().get(0).puntaje(), "la mas reciente primero");
        assertEquals(1, publico.publicacionesActivas().size(), "tope configurado");
    }

    @Test
    @DisplayName("un perfil nuevo tiene perfil publico vacio: sin calificaciones ni publicaciones")
    void perfilPublicoVacio() {
        PerfilDto ana = e.profesional("u-ana", "Ana");

        PerfilPublicoDto publico = app.perfilUseCase.verPerfilPublico(ana.id());

        assertEquals(0.0, publico.reputacion(), 0.0001);
        assertTrue(publico.ultimasCalificaciones().isEmpty());
        assertTrue(publico.publicacionesActivas().isEmpty());
    }

    @Test
    @DisplayName("cada operacion que escribe corre en una transaccion")
    void usaTransacciones() {
        app.identidad.agregarUsuario("u-ana", "Ana");
        app.identidad.iniciarSesion("u-ana");
        int antes = app.transaccion.ejecuciones();

        PerfilDto ana = app.perfilUseCase.registrar(comandoDe("u-ana", "Ana"));
        assertEquals(antes + 1, app.transaccion.ejecuciones());
        app.perfilUseCase.obtener(ana.id());
        assertEquals(antes + 1, app.transaccion.ejecuciones(), "las lecturas no abren transaccion propia");
    }
}
