package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.exception.ErrorCode;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.domain.query.PaginaRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Unitario y sin base: cada helper de ValidadorEntrada acepta lo valido y rechaza lo invalido con VALIDACION. */
@DisplayName("ValidadorEntrada: obligatorios, formatos y rangos de la entrada de los controllers")
class ValidadorEntradaTest {

    private static final LocalDateTime LUNES = LocalDateTime.of(2030, 1, 7, 10, 0);

    /** Exige que la validacion falle con ValidacionException (codigo VALIDACION) y devuelve el mensaje. */
    private static String rechaza(Executable validacion) {
        ValidacionException e = assertThrows(ValidacionException.class, validacion);
        assertEquals(ErrorCode.VALIDACION, e.getCodigo());
        return e.getMessage();
    }

    // ----- obligatorios -----

    @Test
    @DisplayName("exigir rechaza null y nombra el dato que falta")
    void exigir() {
        assertDoesNotThrow(() -> ValidadorEntrada.exigir("algo", "el dato"));
        assertEquals("Falta indicar el dato", rechaza(() -> ValidadorEntrada.exigir(null, "el dato")));
    }

    @Test
    @DisplayName("exigirPagina rechaza una pagina nula")
    void exigirPagina() {
        assertDoesNotThrow(() -> ValidadorEntrada.exigirPagina(PaginaRequest.de(0, 10)));
        assertEquals("Falta indicar la pagina", rechaza(() -> ValidadorEntrada.exigirPagina(null)));
    }

    @Test
    @DisplayName("exigirTexto rechaza null, vacio, en blanco y textos mas largos que el maximo")
    void exigirTexto() {
        assertDoesNotThrow(() -> ValidadorEntrada.exigirTexto("hola", 4, "el texto"));
        rechaza(() -> ValidadorEntrada.exigirTexto(null, 4, "el texto"));
        rechaza(() -> ValidadorEntrada.exigirTexto("", 4, "el texto"));
        rechaza(() -> ValidadorEntrada.exigirTexto("   ", 4, "el texto"));
        assertTrue(rechaza(() -> ValidadorEntrada.exigirTexto("hola!", 4, "el texto")).contains("4 caracteres"));
    }

    // ----- formatos -----

    @Test
    @DisplayName("exigirId rechaza null, cero y negativos")
    void exigirId() {
        assertDoesNotThrow(() -> ValidadorEntrada.exigirId(1L, "la publicacion"));
        assertEquals("Falta indicar la publicacion", rechaza(() -> ValidadorEntrada.exigirId(null, "la publicacion")));
        assertTrue(rechaza(() -> ValidadorEntrada.exigirId(0L, "la publicacion")).contains("id positivo"));
        rechaza(() -> ValidadorEntrada.exigirId(-5L, "la publicacion"));
    }

    @Test
    @DisplayName("exigirLargo acepta null y el largo justo, y rechaza uno mas")
    void exigirLargo() {
        assertDoesNotThrow(() -> ValidadorEntrada.exigirLargo(null, 3, "el titulo"));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirLargo("abc", 3, "el titulo"));
        assertEquals("Formato invalido: el titulo supera los 3 caracteres",
                rechaza(() -> ValidadorEntrada.exigirLargo("abcd", 3, "el titulo")));
    }

    @Test
    @DisplayName("exigirTextos acepta null o vacio y rechaza elementos nulos, en blanco o demasiado largos")
    void exigirTextos() {
        assertDoesNotThrow(() -> ValidadorEntrada.exigirTextos(null, 5, "las etiquetas"));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirTextos(Set.of(), 5, "las etiquetas"));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirTextos(Set.of("java", "sql"), 5, "las etiquetas"));
        rechaza(() -> ValidadorEntrada.exigirTextos(Arrays.asList("java", null), 5, "las etiquetas"));
        rechaza(() -> ValidadorEntrada.exigirTextos(List.of("java", "  "), 5, "las etiquetas"));
        rechaza(() -> ValidadorEntrada.exigirTextos(List.of("javascript"), 5, "las etiquetas"));
    }

    @Test
    @DisplayName("exigirZonaHoraria acepta vacio (sin cambios) y zonas reales, y rechaza las inexistentes")
    void exigirZonaHoraria() {
        assertDoesNotThrow(() -> ValidadorEntrada.exigirZonaHoraria(null));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirZonaHoraria("  "));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirZonaHoraria("America/Argentina/Buenos_Aires"));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirZonaHoraria(" UTC "));
        assertTrue(rechaza(() -> ValidadorEntrada.exigirZonaHoraria("Marte/Olympus")).contains("Marte/Olympus"));
    }

    // ----- rangos -----

    @Test
    @DisplayName("exigirNoNegativo acepta 0 y positivos (y un monto nulo) y rechaza negativos")
    void exigirNoNegativo() {
        assertDoesNotThrow(() -> ValidadorEntrada.exigirNoNegativo(0, "el cupo"));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirNoNegativo(3, "el cupo"));
        rechaza(() -> ValidadorEntrada.exigirNoNegativo(-1, "el cupo"));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirNoNegativo((BigDecimal) null, "el precio"));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirNoNegativo(BigDecimal.ZERO, "el precio"));
        rechaza(() -> ValidadorEntrada.exigirNoNegativo(new BigDecimal("-0.01"), "el precio"));
    }

    @Test
    @DisplayName("exigirPositivo rechaza null, cero y negativos")
    void exigirPositivo() {
        assertDoesNotThrow(() -> ValidadorEntrada.exigirPositivo(new BigDecimal("0.01"), "el valor"));
        rechaza(() -> ValidadorEntrada.exigirPositivo(null, "el valor"));
        rechaza(() -> ValidadorEntrada.exigirPositivo(BigDecimal.ZERO, "el valor"));
        rechaza(() -> ValidadorEntrada.exigirPositivo(BigDecimal.valueOf(-3), "el valor"));
    }

    @Test
    @DisplayName("exigirMaximo acepta null y el maximo justo, y rechaza lo que lo supera")
    void exigirMaximo() {
        assertDoesNotThrow(() -> ValidadorEntrada.exigirMaximo(null, 100, "el porcentaje"));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirMaximo(BigDecimal.valueOf(100), 100, "el porcentaje"));
        rechaza(() -> ValidadorEntrada.exigirMaximo(new BigDecimal("100.5"), 100, "el porcentaje"));
    }

    @Test
    @DisplayName("exigirEntre (entero) incluye los extremos")
    void exigirEntreEntero() {
        assertDoesNotThrow(() -> ValidadorEntrada.exigirEntre(1, 1, 5, "el puntaje"));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirEntre(5, 1, 5, "el puntaje"));
        assertEquals("Fuera de rango: el puntaje debe estar entre 1 y 5",
                rechaza(() -> ValidadorEntrada.exigirEntre(0, 1, 5, "el puntaje")));
        rechaza(() -> ValidadorEntrada.exigirEntre(6, 1, 5, "el puntaje"));
    }

    @Test
    @DisplayName("exigirEntre (decimal opcional) acepta null y rechaza valores afuera y NaN")
    void exigirEntreDecimal() {
        assertDoesNotThrow(() -> ValidadorEntrada.exigirEntre((Double) null, 0, 5, "la reputacion"));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirEntre(4.5, 0, 5, "la reputacion"));
        rechaza(() -> ValidadorEntrada.exigirEntre(-0.1, 0, 5, "la reputacion"));
        rechaza(() -> ValidadorEntrada.exigirEntre(5.1, 0, 5, "la reputacion"));
        rechaza(() -> ValidadorEntrada.exigirEntre(Double.NaN, 0, 5, "la reputacion"));
    }

    @Test
    @DisplayName("exigirAnterior (fecha y hora) acepta extremos nulos y rechaza inicio igual o posterior al fin")
    void exigirAnteriorFechaHora() {
        assertDoesNotThrow(() -> ValidadorEntrada.exigirAnterior(LUNES, LUNES.plusHours(1), "la franja"));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirAnterior(null, LUNES, "la franja"));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirAnterior(LUNES, null, "la franja"));
        rechaza(() -> ValidadorEntrada.exigirAnterior(LUNES, LUNES, "la franja"));
        assertEquals("Fuera de rango: en la franja, el inicio debe ser anterior al fin",
                rechaza(() -> ValidadorEntrada.exigirAnterior(LUNES.plusHours(1), LUNES, "la franja")));
    }

    @Test
    @DisplayName("exigirAnterior (hora del dia) rechaza un bloque invertido")
    void exigirAnteriorHora() {
        LocalTime nueve = LocalTime.of(9, 0);
        assertDoesNotThrow(() -> ValidadorEntrada.exigirAnterior(nueve, nueve.plusHours(8), "el bloque"));
        assertDoesNotThrow(() -> ValidadorEntrada.exigirAnterior((LocalTime) null, nueve, "el bloque"));
        rechaza(() -> ValidadorEntrada.exigirAnterior(nueve, nueve, "el bloque"));
        rechaza(() -> ValidadorEntrada.exigirAnterior(nueve.plusHours(1), nueve, "el bloque"));
    }
}
