package com.uade.skillswap.domain.model.vo;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Value Objects: Habilidad, Puntaje, RangoPrecio, BloqueDisponibilidad")
class ValueObjectsTest {

    // ----- Habilidad -----

    @Test
    @DisplayName("Habilidad normaliza el nombre: minusculas y sin espacios sobrantes")
    void habilidadNormaliza() {
        assertEquals("java", Habilidad.de("  JaVa ").getNombre());
        assertEquals(Habilidad.de("Java"), Habilidad.de(" java"));
        assertEquals(Habilidad.de("Java").hashCode(), Habilidad.de("JAVA").hashCode());
        assertEquals("java", Habilidad.de("Java").toString());
    }

    @Test
    @DisplayName("Habilidad rechaza nombres nulos o en blanco")
    void habilidadInvalida() {
        assertThrows(ReglaDeNegocioException.class, () -> Habilidad.de(null));
        assertThrows(ReglaDeNegocioException.class, () -> Habilidad.de("   "));
    }

    // ----- Puntaje -----

    @Test
    @DisplayName("Puntaje acepta de 1 a 5 inclusive")
    void puntajeValido() {
        for (int i = 1; i <= 5; i++) {
            assertEquals(i, Puntaje.de(i).getValor());
        }
        assertEquals(Puntaje.de(3), Puntaje.de(3));
        assertNotEquals(Puntaje.de(3), Puntaje.de(4));
    }

    @Test
    @DisplayName("Puntaje rechaza 0, negativos y valores mayores a 5")
    void puntajeFueraDeRango() {
        assertThrows(ReglaDeNegocioException.class, () -> Puntaje.de(0));
        assertThrows(ReglaDeNegocioException.class, () -> Puntaje.de(6));
        assertThrows(ReglaDeNegocioException.class, () -> Puntaje.de(-2));
    }

    // ----- RangoPrecio -----

    @Test
    @DisplayName("RangoPrecio incluye los extremos y excluye lo que queda afuera")
    void rangoIncluye() {
        RangoPrecio r = new RangoPrecio(SkillCoins.de(10), SkillCoins.de(20));
        assertTrue(r.incluye(SkillCoins.de(10)));
        assertTrue(r.incluye(SkillCoins.de(15)));
        assertTrue(r.incluye(SkillCoins.de(20)));
        assertFalse(r.incluye(SkillCoins.de("9.99")));
        assertFalse(r.incluye(SkillCoins.de("20.01")));
    }

    @Test
    @DisplayName("RangoPrecio admite un extremo abierto (null)")
    void rangoAbierto() {
        RangoPrecio soloMinimo = new RangoPrecio(SkillCoins.de(10), null);
        assertTrue(soloMinimo.incluye(SkillCoins.de(1000)));
        assertFalse(soloMinimo.incluye(SkillCoins.de(5)));
        RangoPrecio soloMaximo = new RangoPrecio(null, SkillCoins.de(10));
        assertTrue(soloMaximo.incluye(SkillCoins.CERO));
        assertFalse(soloMaximo.incluye(SkillCoins.de(11)));
        assertTrue(new RangoPrecio(null, null).incluye(SkillCoins.de(1)));
    }

    @Test
    @DisplayName("RangoPrecio rechaza un minimo mayor al maximo")
    void rangoInvertido() {
        assertThrows(ReglaDeNegocioException.class, () -> new RangoPrecio(SkillCoins.de(20), SkillCoins.de(10)));
    }

    // ----- BloqueDisponibilidad -----

    @Test
    @DisplayName("BloqueDisponibilidad valido y con igualdad por valor")
    void bloqueValido() {
        BloqueDisponibilidad a = new BloqueDisponibilidad(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        BloqueDisponibilidad b = new BloqueDisponibilidad(DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0));
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertEquals(DayOfWeek.MONDAY, a.getDia());
        assertEquals(LocalTime.of(9, 0), a.getDesde());
        assertEquals(LocalTime.of(12, 0), a.getHasta());
        assertNotEquals(a, new BloqueDisponibilidad(DayOfWeek.TUESDAY, LocalTime.of(9, 0), LocalTime.of(12, 0)));
    }

    @Test
    @DisplayName("BloqueDisponibilidad exige desde anterior a hasta y todos los datos")
    void bloqueInvalido() {
        assertThrows(ReglaDeNegocioException.class,
                () -> new BloqueDisponibilidad(DayOfWeek.MONDAY, LocalTime.of(12, 0), LocalTime.of(12, 0)));
        assertThrows(ReglaDeNegocioException.class,
                () -> new BloqueDisponibilidad(DayOfWeek.MONDAY, LocalTime.of(13, 0), LocalTime.of(12, 0)));
        assertThrows(ReglaDeNegocioException.class,
                () -> new BloqueDisponibilidad(null, LocalTime.of(9, 0), LocalTime.of(12, 0)));
        assertThrows(ReglaDeNegocioException.class,
                () -> new BloqueDisponibilidad(DayOfWeek.MONDAY, null, LocalTime.of(12, 0)));
    }
}
