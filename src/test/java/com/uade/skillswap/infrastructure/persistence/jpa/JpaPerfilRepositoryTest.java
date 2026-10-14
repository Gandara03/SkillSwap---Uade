package com.uade.skillswap.infrastructure.persistence.jpa;

import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.vo.BloqueDisponibilidad;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.PerfilJpaEntity;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaPerfilRepositoryTest extends JpaTestBase {

    private Perfil perfilCompleto() {
        Perfil p = Perfil.crear("u-ana", "Ana", TipoPerfil.PROFESIONAL, ZoneId.of("America/Argentina/Buenos_Aires"), AHORA);
        p.actualizarDatos("Ana Perez", "Backend developer", ZoneId.of("Europe/Madrid"),
                List.of(Habilidad.de("Java"), Habilidad.de("SQL")), List.of("ia", "musica"),
                List.of(new BloqueDisponibilidad(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)),
                        new BloqueDisponibilidad(DayOfWeek.FRIDAY, LocalTime.of(14, 30), LocalTime.of(18, 0))),
                CanalNotificacion.PUSH);
        return p;
    }

    @Test
    @DisplayName("guardar y recuperar un perfil conserva todos sus datos y colecciones")
    void guardarYRecuperar() {
        Perfil guardado = perfiles.guardar(perfilCompleto());

        Perfil leido = perfiles.buscarPorId(guardado.getId()).orElseThrow();
        assertEquals("u-ana", leido.getUserId());
        assertEquals(TipoPerfil.PROFESIONAL, leido.getTipo());
        assertEquals("Ana Perez", leido.getNombreVisible());
        assertEquals("Backend developer", leido.getBiografia());
        assertEquals(ZoneId.of("Europe/Madrid"), leido.getZonaHoraria());
        assertEquals(Set.of(Habilidad.de("java"), Habilidad.de("sql")), leido.getHabilidades());
        assertEquals(Set.of("ia", "musica"), leido.getIntereses());
        assertEquals(CanalNotificacion.PUSH, leido.getCanalPreferido());
        assertEquals(0.0, leido.getReputacion());
        assertEquals(0, leido.getCantidadCalificaciones());
        assertTrue(leido.estaActivo());
        assertEquals(AHORA, leido.getCreadoEn());
    }

    @Test
    @DisplayName("la disponibilidad semanal conserva el orden de los bloques")
    void disponibilidadConservaElOrden() {
        Perfil guardado = perfiles.guardar(perfilCompleto());

        List<BloqueDisponibilidad> bloques = perfiles.buscarPorId(guardado.getId()).orElseThrow().getDisponibilidad();
        assertEquals(2, bloques.size());
        assertEquals(DayOfWeek.MONDAY, bloques.get(0).getDia());
        assertEquals(LocalTime.of(9, 0), bloques.get(0).getDesde());
        assertEquals(LocalTime.of(12, 0), bloques.get(0).getHasta());
        assertEquals(DayOfWeek.FRIDAY, bloques.get(1).getDia());
        assertEquals(LocalTime.of(14, 30), bloques.get(1).getDesde());
    }

    @Test
    @DisplayName("guardar asigna id y version a la instancia recibida y devuelve esa misma instancia")
    void guardarAsignaIdYVersion() {
        Perfil nuevo = Perfil.crear("u-bruno", "Bruno", TipoPerfil.PROFESIONAL, ZoneId.of("UTC"), AHORA);
        assertNull(nuevo.getId());

        Perfil guardado = perfiles.guardar(nuevo);

        assertTrue(guardado == nuevo);
        assertNotNull(nuevo.getId());
        assertEquals(Long.valueOf(0), nuevo.getVersion());
    }

    @Test
    @DisplayName("actualizar incrementa la version y sincroniza las colecciones (agrega y quita)")
    void actualizarSincronizaColecciones() {
        Perfil guardado = perfiles.guardar(perfilCompleto());

        Perfil editable = perfiles.buscarPorId(guardado.getId()).orElseThrow();
        editable.actualizarDatos("Ana P.", null, null, List.of(Habilidad.de("java"), Habilidad.de("docker")),
                List.of("ia"), List.of(), CanalNotificacion.EMAIL);
        perfiles.guardar(editable);

        Perfil leido = perfiles.buscarPorId(guardado.getId()).orElseThrow();
        assertEquals(Long.valueOf(1), leido.getVersion());
        assertEquals("Ana P.", leido.getNombreVisible());
        assertEquals(Set.of(Habilidad.de("java"), Habilidad.de("docker")), leido.getHabilidades());
        assertEquals(Set.of("ia"), leido.getIntereses());
        assertTrue(leido.getDisponibilidad().isEmpty());
        assertEquals(CanalNotificacion.EMAIL, leido.getCanalPreferido());
    }

    @Test
    @DisplayName("guardar sin cambios no toca la fila ni incrementa la version")
    void guardarSinCambiosNoIncrementaLaVersion() {
        Perfil guardado = perfiles.guardar(perfilCompleto());

        Perfil leido = perfiles.buscarPorId(guardado.getId()).orElseThrow();
        perfiles.guardar(leido);

        assertEquals(Long.valueOf(0), perfiles.buscarPorId(guardado.getId()).orElseThrow().getVersion());
    }

    @Test
    @DisplayName("la baja logica y la reputacion se persisten")
    void bajaLogicaYReputacion() {
        Perfil ana = profesional("u-ana");
        Perfil conRep = conReputacion(ana, 4.5, 2);
        assertEquals(4.5, perfiles.buscarPorId(ana.getId()).orElseThrow().getReputacion());
        assertEquals(2, perfiles.buscarPorId(ana.getId()).orElseThrow().getCantidadCalificaciones());

        conRep.darDeBaja();
        perfiles.guardar(conRep);

        assertFalse(perfiles.buscarPorId(ana.getId()).orElseThrow().estaActivo());
    }

    @Test
    @DisplayName("el userId es unico: un segundo perfil con el mismo userId viola la restriccion")
    void userIdUnico() {
        profesional("u-ana");

        assertThrows(PersistenceException.class, () -> profesional("u-ana"));
        assertEquals(1, contarFilas(ctx, PerfilJpaEntity.class));
    }

    @Test
    @DisplayName("buscarPorUserId encuentra por userId y devuelve vacio si no existe")
    void buscarPorUserId() {
        Perfil ana = profesional("u-ana");

        assertEquals(ana.getId(), perfiles.buscarPorUserId("u-ana").orElseThrow().getId());
        assertTrue(perfiles.buscarPorUserId("u-nadie").isEmpty());
        assertTrue(perfiles.buscarPorUserId(null).isEmpty());
    }

    @Test
    @DisplayName("buscarPorId devuelve vacio si no existe o si el id es null")
    void buscarPorIdInexistente() {
        assertTrue(perfiles.buscarPorId(9999L).isEmpty());
        assertTrue(perfiles.buscarPorId(null).isEmpty());
    }

    @Test
    @DisplayName("buscarPorIds trae varios perfiles con sus colecciones en lote, ignorando ids inexistentes")
    void buscarPorIds() {
        Perfil ana = perfiles.guardar(perfilCompleto());
        Perfil bruno = profesional("u-bruno");
        profesional("u-cecilia");

        List<Perfil> encontrados = perfiles.buscarPorIds(Set.of(ana.getId(), bruno.getId(), 9999L));

        assertEquals(2, encontrados.size());
        assertIterableEquals(List.of(ana.getId(), bruno.getId()), encontrados.stream().map(Perfil::getId).toList());
        assertEquals(2, encontrados.get(0).getHabilidades().size());
        assertEquals(2, encontrados.get(0).getDisponibilidad().size());
        assertTrue(perfiles.buscarPorIds(Set.of()).isEmpty());
        assertTrue(perfiles.buscarPorIds(null).isEmpty());
    }
}
