package com.uade.skillswap.application.service;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.exception.ErrorCode;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("SesionService: actor actual a partir de la identidad externa")
class SesionServiceTest {

    private Escenario e;
    private AplicacionEnMemoria app;

    @BeforeEach
    void preparar() {
        e = new Escenario();
        app = e.app;
    }

    @Test
    @DisplayName("con sesion iniciada y perfil activo devuelve el id del perfil del usuario")
    void perfilDelUsuarioAutenticado() {
        PerfilDto ana = e.profesional("u-ana", "Ana");
        PerfilDto bruno = e.profesional("u-bruno", "Bruno");

        app.identidad.iniciarSesion("u-ana");
        assertEquals(ana.id(), app.sesion.perfilIdActual());
        app.identidad.iniciarSesion("u-bruno");
        assertEquals(bruno.id(), app.sesion.perfilIdActual());
    }

    @Test
    @DisplayName("sin sesion iniciada lanza NoAutorizadoException")
    void sinSesion() {
        e.profesional("u-ana", "Ana");
        app.identidad.cerrarSesion();

        NoAutorizadoException ex = assertThrows(NoAutorizadoException.class, () -> app.sesion.perfilIdActual());
        assertEquals(ErrorCode.NO_AUTORIZADO, ex.getCodigo());
    }

    @Test
    @DisplayName("un usuario autenticado que todavia no registro perfil no puede operar")
    void usuarioSinPerfil() {
        app.identidad.agregarUsuario("u-nuevo", "Nuevo");
        app.identidad.iniciarSesion("u-nuevo");

        assertThrows(NoAutorizadoException.class, () -> app.sesion.perfilIdActual());
    }

    @Test
    @DisplayName("un usuario inactivo en el servicio de identidad no puede operar aunque tenga perfil")
    void usuarioInactivoEnIdentidad() {
        e.profesional("u-ana", "Ana");
        app.identidad.agregarUsuario("u-ana", "Ana", "u-ana@test.local", false);
        app.identidad.iniciarSesion("u-ana");

        assertThrows(NoAutorizadoException.class, () -> app.sesion.perfilIdActual());
    }

    @Test
    @DisplayName("un perfil dado de baja deja de resolverse como actor")
    void perfilDeBaja() {
        PerfilDto ana = e.profesional("u-ana", "Ana");
        app.perfilUseCase.darDeBaja(ana.id());

        assertThrows(NoAutorizadoException.class, () -> app.sesion.perfilIdActual());
    }
}
