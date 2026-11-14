package com.uade.skillswap.domain.model.vo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Textos: comparacion sin mayusculas ni acentos")
class TextosTest {

    @Test
    @DisplayName("la clave pasa a minusculas y quita los acentos, pero no cambia otras letras ni los espacios")
    void clave() {
        assertEquals("java", Textos.clave("JaVa"));
        assertEquals("diseno", Textos.clave("Diseño"));
        assertEquals("programacion", Textos.clave("PROGRAMACIÓN"));
        assertEquals("ux", Textos.clave("Úx"));
        assertNotEquals(Textos.clave("java"), Textos.clave("java "), "los espacios no se tocan");
        assertNotEquals(Textos.clave("java"), Textos.clave("javas"));
    }

    @Test
    @DisplayName("sinRepetir deja el primero de cada grupo, en el orden recibido; null da un conjunto vacio")
    void sinRepetir() {
        assertEquals(List.of("Diseño", "Java", "sql"),
                List.copyOf(Textos.sinRepetir(List.of("Diseño", "Java", "diseno", "JAVA", "sql", "DISEÑO"))));
        assertTrue(Textos.sinRepetir(null).isEmpty());
        assertTrue(Textos.sinRepetir(List.of()).isEmpty());
    }
}
