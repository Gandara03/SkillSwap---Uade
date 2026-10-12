package com.uade.skillswap.domain.model.perfil;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.model.calificacion.Calificacion;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.vo.BloqueDisponibilidad;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.support.Datos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Perfil")
class PerfilTest {

    private Perfil nuevo() {
        return Perfil.crear("u-ana", "Ana", TipoPerfil.PROFESIONAL, ZoneId.of("America/Argentina/Buenos_Aires"), Datos.AHORA);
    }

    @Test
    @DisplayName("se crea activo, con canal INTERNA, reputacion 0 y sin calificaciones")
    void creacionPorDefecto() {
        Perfil p = nuevo();
        assertNull(p.getId());
        assertEquals("u-ana", p.getUserId());
        assertEquals(TipoPerfil.PROFESIONAL, p.getTipo());
        assertTrue(p.estaActivo());
        assertEquals(CanalNotificacion.INTERNA, p.getCanalPreferido());
        assertEquals(0.0, p.getReputacion(), 0.0001);
        assertEquals(0, p.getCantidadCalificaciones());
        assertEquals(Datos.AHORA, p.getCreadoEn());
    }

    @Test
    @DisplayName("recorta el nombre y el userId; zona nula pasa a UTC")
    void normalizaDatos() {
        Perfil p = Perfil.crear("  u-x ", "  Ana Gomez ", TipoPerfil.ORGANIZACION, null, Datos.AHORA);
        assertEquals("u-x", p.getUserId());
        assertEquals("Ana Gomez", p.getNombreVisible());
        assertEquals(ZoneId.of("UTC"), p.getZonaHoraria());
    }

    @Test
    @DisplayName("exige userId, tipo y nombre visible")
    void camposObligatorios() {
        assertThrows(ReglaDeNegocioException.class, () -> Perfil.crear(null, "Ana", TipoPerfil.PROFESIONAL, null, Datos.AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> Perfil.crear("  ", "Ana", TipoPerfil.PROFESIONAL, null, Datos.AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> Perfil.crear("u", "Ana", null, null, Datos.AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> Perfil.crear("u", " ", TipoPerfil.PROFESIONAL, null, Datos.AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> Perfil.crear("u", null, TipoPerfil.PROFESIONAL, null, Datos.AHORA));
    }

    @Test
    @DisplayName("actualizarDatos reemplaza habilidades, intereses, disponibilidad y canal preferido")
    void actualizarDatos() {
        Perfil p = nuevo();
        BloqueDisponibilidad bloque = new BloqueDisponibilidad(DayOfWeek.FRIDAY, LocalTime.of(10, 0), LocalTime.of(14, 0));
        p.actualizarDatos("Ana G.", "Backend dev", ZoneId.of("UTC"), List.of(Habilidad.de("Java"), Habilidad.de("SQL")),
                List.of("arquitectura", "testing"), List.of(bloque), CanalNotificacion.EMAIL);

        assertEquals("Ana G.", p.getNombreVisible());
        assertEquals("Backend dev", p.getBiografia());
        assertEquals(ZoneId.of("UTC"), p.getZonaHoraria());
        assertEquals(2, p.getHabilidades().size());
        assertTrue(p.getHabilidades().contains(Habilidad.de("sql")));
        assertTrue(p.getIntereses().contains("testing"));
        assertEquals(List.of(bloque), p.getDisponibilidad());
        assertEquals(CanalNotificacion.EMAIL, p.getCanalPreferido());

        p.actualizarDatos("Ana G.", null, null, List.of(Habilidad.de("Go")), null, null, null);
        assertEquals(1, p.getHabilidades().size());
        assertTrue(p.getIntereses().isEmpty());
        assertTrue(p.getDisponibilidad().isEmpty());
        assertEquals(CanalNotificacion.EMAIL, p.getCanalPreferido(), "canal nulo no pisa la preferencia");
        assertEquals(ZoneId.of("UTC"), p.getZonaHoraria(), "zona nula no pisa la zona actual");
    }

    @Test
    @DisplayName("actualizarDatos exige nombre visible")
    void actualizarSinNombre() {
        Perfil p = nuevo();
        assertThrows(ReglaDeNegocioException.class,
                () -> p.actualizarDatos(" ", null, null, List.of(), List.of(), List.of(), null));
        assertEquals("Ana", p.getNombreVisible());
    }

    @Test
    @DisplayName("las colecciones expuestas son de solo lectura")
    void coleccionesInmutables() {
        Perfil p = nuevo();
        assertThrows(UnsupportedOperationException.class, () -> p.getHabilidades().add(Habilidad.de("x")));
        assertThrows(UnsupportedOperationException.class, () -> p.getIntereses().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> p.getDisponibilidad().clear());
    }

    @Test
    @DisplayName("la reputacion se recalcula como promedio de los puntajes recibidos")
    void reputacionPromedio() {
        Perfil proveedor = Datos.profesional(Datos.PROVEEDOR, "u-prov");
        Contratacion c = Datos.completada();
        Calificacion c1 = Datos.calificacion(c, Datos.CLIENTE, 5);
        Calificacion c2 = Calificacion.registrar(c, Datos.CLIENTE,
                com.uade.skillswap.domain.model.vo.Puntaje.de(2), "regular", Datos.AHORA);
        proveedor.recalcularReputacion(List.of(c1, c2));
        assertEquals(3.5, proveedor.getReputacion(), 0.0001);
        assertEquals(2, proveedor.getCantidadCalificaciones());
    }

    @Test
    @DisplayName("recalcular es idempotente: no acumula sobre el valor anterior")
    void reputacionPorRecalculo() {
        Perfil proveedor = Datos.profesional(Datos.PROVEEDOR, "u-prov");
        Calificacion cinco = Datos.calificacion(Datos.completada(), Datos.CLIENTE, 5);
        proveedor.recalcularReputacion(List.of(cinco));
        proveedor.recalcularReputacion(List.of(cinco));
        assertEquals(5.0, proveedor.getReputacion(), 0.0001);
        assertEquals(1, proveedor.getCantidadCalificaciones());
    }

    @Test
    @DisplayName("sin calificaciones la reputacion vuelve a 0")
    void reputacionSinCalificaciones() {
        Perfil proveedor = Datos.profesional(Datos.PROVEEDOR, "u-prov");
        proveedor.recalcularReputacion(List.of(Datos.calificacion(Datos.completada(), Datos.CLIENTE, 4)));
        proveedor.recalcularReputacion(List.of());
        assertEquals(0.0, proveedor.getReputacion(), 0.0001);
        assertEquals(0, proveedor.getCantidadCalificaciones());
    }

    @Test
    @DisplayName("baja logica: queda inactivo, conserva sus datos y ya no puede actualizarse")
    void bajaLogica() {
        Perfil p = nuevo();
        p.darDeBaja();
        assertFalse(p.estaActivo());
        assertEquals("Ana", p.getNombreVisible());
        assertEquals("u-ana", p.getUserId());
        assertThrows(ReglaDeNegocioException.class,
                () -> p.actualizarDatos("Otro", null, null, List.of(), List.of(), List.of(), null));
    }

    @Test
    @DisplayName("reconstruir rehidrata todos los campos tal cual vienen de persistencia")
    void reconstruir() {
        Perfil p = Perfil.reconstruir(9L, "u-z", TipoPerfil.ORGANIZACION, "Org", "bio", ZoneId.of("UTC"),
                List.of(Habilidad.de("a")), List.of("i"), List.of(), CanalNotificacion.PUSH, 4.25, 8, false,
                Datos.AHORA, 3L);
        assertEquals(9L, (long) p.getId());
        assertEquals(4.25, p.getReputacion(), 0.0001);
        assertEquals(8, p.getCantidadCalificaciones());
        assertFalse(p.estaActivo());
        assertEquals(CanalNotificacion.PUSH, p.getCanalPreferido());
        assertEquals(3L, (long) p.getVersion());
    }
}
