package com.uade.skillswap.domain.model.vo;

import java.text.Normalizer;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Comparacion de los textos que carga el usuario (etiquetas e intereses) sin distinguir mayusculas ni acentos.
 * La base compara asi esas columnas: dos valores que solo difieren en eso son la misma clave primaria, y guardar
 * los dos haria fallar toda la operacion.
 */
public final class Textos {

    private Textos() {
    }

    /** Clave de comparacion: sin acentos y en minusculas. */
    public static String clave(String texto) {
        // NFD separa cada letra de su acento; \p{M} son esas marcas sueltas, que se borran.
        String sinAcentos = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return sinAcentos.toLowerCase(Locale.ROOT);
    }

    /** Los textos sin repetidos segun {@link #clave}: de cada grupo queda el primero, en el orden recibido. */
    public static Set<String> sinRepetir(Collection<String> textos) {
        Set<String> resultado = new LinkedHashSet<>();
        if (textos == null) {
            return resultado;
        }
        Set<String> claves = new HashSet<>();
        for (String texto : textos) {
            if (claves.add(clave(texto))) {
                resultado.add(texto);
            }
        }
        return resultado;
    }
}
