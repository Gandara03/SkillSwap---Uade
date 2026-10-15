package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.ActualizarPerfilCommand;
import com.uade.skillswap.application.command.BloqueDisponibilidadDto;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PerfilPublicoDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static com.uade.skillswap.application.exception.ErrorCode.NO_AUTORIZADO;
import static com.uade.skillswap.application.exception.ErrorCode.NO_ENCONTRADO;
import static com.uade.skillswap.application.exception.ErrorCode.VALIDACION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Integracion PerfilController -> PerfilUseCase / PerfilPublicoUseCase -> JPA/H2. */
@DisplayName("PerfilController (JPA/H2): registrar, actualizar, perfil publico y baja")
class PerfilControllerTest extends ControllerJpaTestBase {

    private static final BloqueDisponibilidadDto LUNES_MANANA =
            new BloqueDisponibilidadDto(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(13, 0));

    private IPerfilController perfiles() {
        return app.getPerfilController();
    }

    private static RegistrarPerfilCommand registro(String userId, String nombre, String zona) {
        return new RegistrarPerfilCommand(userId, nombre, TipoPerfil.PROFESIONAL, zona, "Bio", Set.of("java"),
                Set.of("sql"), List.of(), CanalNotificacion.INTERNA);
    }

    private static ActualizarPerfilCommand cambios(String nombre, String zona, List<BloqueDisponibilidadDto> bloques) {
        return new ActualizarPerfilCommand(nombre, "Nueva bio", zona, Set.of("java", "spring"), Set.of("arquitectura"),
                bloques, CanalNotificacion.EMAIL);
    }

    // ---------- camino feliz ----------

    @Test
    @DisplayName("registrar, ver mi perfil y actualizarlo: los cambios quedan guardados")
    void registrarYActualizar() {
        como("u-ana");
        PerfilDto registrado = ok(perfiles().registrar(registro("u-ana", "Ana", "America/Argentina/Buenos_Aires")));
        assertEquals("Ana", registrado.nombreVisible());
        assertEquals(registrado.id(), ok(perfiles().miPerfil()).id());

        ok(perfiles().actualizar(cambios("Ana Gomez", "UTC", List.of(LUNES_MANANA))));

        PerfilDto actual = ok(perfiles().miPerfil());
        assertEquals("Ana Gomez", actual.nombreVisible());
        assertEquals("UTC", actual.zonaHoraria());
        assertEquals(Set.of("java", "spring"), actual.habilidades());
        assertEquals(Set.of("arquitectura"), actual.intereses());
        assertEquals(List.of(LUNES_MANANA), actual.disponibilidad());
        assertEquals(CanalNotificacion.EMAIL, actual.canalPreferido());
    }

    @Test
    @DisplayName("verPerfilPublico anda sin sesion: reputacion, ultimas calificaciones y publicaciones activas")
    void verPerfilPublico() {
        PerfilDto ana = profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        PublicacionDto mentoria = publicarMentoria("u-ana", 40, 2, 2);
        ContratacionDto completada = contratarYCompletar("u-ana", "u-bruno", mentoria, 0);
        como("u-bruno");
        ok(app.getCalificacionController().calificar(calificacion(completada.id(), 4)));
        sinSesion();

        PerfilPublicoDto publico = ok(perfiles().verPerfilPublico(ana.id()));

        assertEquals("Ana", publico.nombreVisible());
        assertEquals(4.0, publico.reputacion());
        assertEquals(1, publico.cantidadCalificaciones());
        assertEquals(List.of(4), publico.ultimasCalificaciones().stream().map(c -> c.puntaje()).toList());
        assertEquals(List.of(mentoria.id()),
                publico.publicacionesActivas().stream().map(PublicacionResumenDto::id).toList());
    }

    @Test
    @DisplayName("darDeBaja es logica: el perfil deja de operar y sus publicaciones salen de la busqueda")
    void darDeBaja() {
        profesional("u-ana", "Ana");
        publicarMentoria("u-ana", 40, 2, 1);
        como("u-ana");

        ok(perfiles().darDeBaja());

        assertTrue(falla(NO_AUTORIZADO, perfiles().miPerfil()).contains("dado de baja"));
        assertEquals(0L, ok(app.getBusquedaController().buscar(FiltroBusqueda.vacio(), primeraPagina()))
                .totalElementos());
    }

    // ---------- validacion de entrada ----------

    @Test
    @DisplayName("registrar con datos invalidos es VALIDACION y no crea el perfil")
    void registrarInvalido() {
        como("u-ana");

        falla(VALIDACION, perfiles().registrar(null));
        falla(VALIDACION, perfiles().registrar(registro("u-ana", "  ", "UTC")));
        falla(VALIDACION, perfiles().registrar(registro("u-ana", texto(151), "UTC")));
        falla(VALIDACION, perfiles().registrar(registro("u-ana", "Ana", "Marte/Olympus")));
        falla(VALIDACION, perfiles().registrar(new RegistrarPerfilCommand("u-ana", "Ana", null, "UTC", null,
                Set.of(), Set.of(), List.of(), null)));
        falla(VALIDACION, perfiles().registrar(new RegistrarPerfilCommand("u-ana", "Ana", TipoPerfil.PROFESIONAL,
                "UTC", null, Set.of(" "), Set.of(), List.of(), null)));

        // ninguno llego a guardarse: el usuario sigue sin perfil y puede registrarse bien
        assertTrue(falla(NO_AUTORIZADO, perfiles().miPerfil()).contains("perfil registrado"));
        assertEquals("Ana", ok(perfiles().registrar(registro("u-ana", "Ana", "UTC"))).nombreVisible());
    }

    @Test
    @DisplayName("actualizar con zona inexistente, bloque invertido o biografia larga es VALIDACION y no cambia nada")
    void actualizarInvalido() {
        profesional("u-ana", "Ana");
        BloqueDisponibilidadDto invertido =
                new BloqueDisponibilidadDto(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(9, 0));
        BloqueDisponibilidadDto sinDia = new BloqueDisponibilidadDto(null, LocalTime.of(9, 0), LocalTime.of(13, 0));

        falla(VALIDACION, perfiles().actualizar(null));
        falla(VALIDACION, perfiles().actualizar(cambios("", "UTC", List.of())));
        falla(VALIDACION, perfiles().actualizar(cambios("Ana Gomez", "Marte/Olympus", List.of())));
        falla(VALIDACION, perfiles().actualizar(cambios("Ana Gomez", "UTC", List.of(invertido))));
        falla(VALIDACION, perfiles().actualizar(cambios("Ana Gomez", "UTC", List.of(sinDia))));
        falla(VALIDACION, perfiles().actualizar(new ActualizarPerfilCommand("Ana Gomez", texto(2001), "UTC",
                Set.of(), Set.of(), List.of(), null)));

        PerfilDto igual = ok(perfiles().miPerfil());
        assertEquals("Ana", igual.nombreVisible());
        assertEquals(List.of(), igual.disponibilidad());
    }

    @Test
    @DisplayName("verPerfilPublico con id nulo o no positivo es VALIDACION; con uno inexistente, NO_ENCONTRADO")
    void verPerfilPublicoConIdInvalido() {
        falla(VALIDACION, perfiles().verPerfilPublico(null));
        falla(VALIDACION, perfiles().verPerfilPublico(0L));
        falla(NO_ENCONTRADO, perfiles().verPerfilPublico(999L));
    }

    // ---------- permisos ----------

    @Test
    @DisplayName("registrar un perfil con el userId de otro usuario es NO_AUTORIZADO")
    void registrarConUserIdAjeno() {
        como("u-ana");
        falla(NO_AUTORIZADO, perfiles().registrar(registro("u-bruno", "Bruno", "UTC")));

        como("u-bruno");
        assertTrue(falla(NO_AUTORIZADO, perfiles().miPerfil()).contains("perfil registrado"),
                "a Bruno no se le creo nada");
    }

    @Test
    @DisplayName("sin sesion no se puede registrar, ver mi perfil, actualizar ni darse de baja")
    void sinSesionNoAutorizado() {
        profesional("u-ana", "Ana");
        sinSesion();

        falla(NO_AUTORIZADO, perfiles().registrar(registro("u-ana", "Ana", "UTC")));
        falla(NO_AUTORIZADO, perfiles().miPerfil());
        falla(NO_AUTORIZADO, perfiles().actualizar(cambios("Otra", "UTC", List.of())));
        falla(NO_AUTORIZADO, perfiles().darDeBaja());
        // primero se resuelve la sesion y despues se mira la entrada
        falla(NO_AUTORIZADO, perfiles().actualizar(null));
    }

    // ---------- otros codigos ----------

    @Test
    @DisplayName("registrar dos veces al mismo usuario es VALIDACION (lo detecta el caso de uso)")
    void registrarDosVeces() {
        profesional("u-ana", "Ana");

        assertTrue(falla(VALIDACION, perfiles().registrar(registro("u-ana", "Ana", "UTC"))).contains("Ya existe"));
    }
}
