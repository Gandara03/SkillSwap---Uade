package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.domain.query.PaginaRequest;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Collection;

/**
 * Validacion de los datos de entrada que reciben los controllers: obligatorios, formatos y rangos.
 * Solo mira los datos que llegaron; no carga nada ni conoce el estado, el actor ni el reloj. Las reglas que dependen
 * de eso (permisos, transiciones, completitud, cupo, saldo) siguen en el dominio y en los casos de uso, que ademas
 * vuelven a validar lo suyo. Cada metodo lanza ValidacionException, que Ejecucion convierte en ErrorCode.VALIDACION.
 */
final class ValidadorEntrada {

    // Largos maximos de los textos: copian los VARCHAR de db/migration/V1__esquema.sql.
    // Si la migracion cambia un largo, hay que cambiarlo tambien aca.
    static final int LARGO_USER_ID = 100;
    static final int LARGO_NOMBRE = 150;
    static final int LARGO_BIOGRAFIA = 2000;
    static final int LARGO_ITEM = 100;             // habilidad, interes o etiqueta
    static final int LARGO_CATEGORIA = 100;
    static final int LARGO_TITULO = 200;
    static final int LARGO_DESCRIPCION = 4000;
    static final int LARGO_MENSAJE = 2000;         // mensaje y objetivo de una solicitud
    static final int LARGO_MOTIVO = 1000;
    static final int LARGO_COMENTARIO = 1000;
    static final int LARGO_DESCRIPCION_PROMOCION = 500;

    // Escala de puntajes y reputacion (la misma que el VO Puntaje) y tope de una promocion por porcentaje.
    static final int PUNTAJE_MINIMO = 1;
    static final int PUNTAJE_MAXIMO = 5;
    static final int PORCENTAJE_MAXIMO = 100;

    private ValidadorEntrada() { }

    // ---------- obligatorios ----------

    static void exigir(Object valor, String nombre) {
        if (valor == null) {
            throw new ValidacionException("Falta indicar " + nombre);
        }
    }

    static void exigirPagina(PaginaRequest pagina) {
        exigir(pagina, "la pagina");
    }

    /** Texto obligatorio: no puede ser null ni estar en blanco, y no puede superar el largo de su columna. */
    static void exigirTexto(String texto, int largoMaximo, String nombre) {
        if (texto == null || texto.isBlank()) {
            throw new ValidacionException("Falta indicar " + nombre);
        }
        exigirLargo(texto, largoMaximo, nombre);
    }

    // ---------- formatos ----------

    /** Los ids los genera la base y siempre son positivos. */
    static void exigirId(Long id, String nombre) {
        exigir(id, nombre);
        if (id <= 0) {
            throw new ValidacionException("Formato invalido: " + nombre + " debe ser un id positivo");
        }
    }

    /** Texto opcional: null esta permitido, pero si viene no puede superar el largo de su columna. */
    static void exigirLargo(String texto, int largoMaximo, String nombre) {
        if (texto != null && texto.length() > largoMaximo) {
            throw new ValidacionException("Formato invalido: " + nombre + " supera los " + largoMaximo + " caracteres");
        }
    }

    /** Coleccion opcional de textos (etiquetas, habilidades, intereses): ningun elemento vacio ni demasiado largo. */
    static void exigirTextos(Collection<String> textos, int largoMaximo, String nombre) {
        if (textos == null) {
            return;
        }
        for (String texto : textos) {
            if (texto == null || texto.isBlank()) {
                throw new ValidacionException("Falta indicar un valor en " + nombre);
            }
            exigirLargo(texto, largoMaximo, "un valor de " + nombre);
        }
    }

    /** Zona horaria opcional (vacia = la de siempre); si viene, tiene que existir. */
    static void exigirZonaHoraria(String zona) {
        if (zona == null || zona.isBlank()) {
            return;
        }
        try {
            ZoneId.of(zona.trim());
        } catch (DateTimeException e) {
            throw new ValidacionException("Formato invalido: la zona horaria '" + zona + "' no existe");
        }
    }

    // ---------- rangos ----------

    static void exigirNoNegativo(int valor, String nombre) {
        if (valor < 0) {
            throw new ValidacionException("Fuera de rango: " + nombre + " no puede ser menor que 0");
        }
    }

    /** Monto opcional: null esta permitido (un borrador puede no tener precio todavia). */
    static void exigirNoNegativo(BigDecimal valor, String nombre) {
        if (valor != null && valor.signum() < 0) {
            throw new ValidacionException("Fuera de rango: " + nombre + " no puede ser menor que 0");
        }
    }

    static void exigirPositivo(BigDecimal valor, String nombre) {
        exigir(valor, nombre);
        if (valor.signum() <= 0) {
            throw new ValidacionException("Fuera de rango: " + nombre + " debe ser mayor que 0");
        }
    }

    static void exigirMaximo(BigDecimal valor, int maximo, String nombre) {
        if (valor != null && valor.compareTo(BigDecimal.valueOf(maximo)) > 0) {
            throw new ValidacionException("Fuera de rango: " + nombre + " no puede superar " + maximo);
        }
    }

    static void exigirEntre(int valor, int minimo, int maximo, String nombre) {
        if (valor < minimo || valor > maximo) {
            throw new ValidacionException(
                    "Fuera de rango: " + nombre + " debe estar entre " + minimo + " y " + maximo);
        }
    }

    /** Valor opcional: null esta permitido. Se escribe con un ! para que un NaN tampoco pase. */
    static void exigirEntre(Double valor, int minimo, int maximo, String nombre) {
        if (valor != null && !(valor >= minimo && valor <= maximo)) {
            throw new ValidacionException(
                    "Fuera de rango: " + nombre + " debe estar entre " + minimo + " y " + maximo);
        }
    }

    /** Si vienen los dos extremos, el inicio tiene que ser anterior al fin. */
    static void exigirAnterior(LocalDateTime inicio, LocalDateTime fin, String nombre) {
        if (inicio != null && fin != null && !inicio.isBefore(fin)) {
            throw new ValidacionException("Fuera de rango: en " + nombre + ", el inicio debe ser anterior al fin");
        }
    }

    static void exigirAnterior(LocalTime inicio, LocalTime fin, String nombre) {
        if (inicio != null && fin != null && !inicio.isBefore(fin)) {
            throw new ValidacionException("Fuera de rango: en " + nombre + ", el inicio debe ser anterior al fin");
        }
    }
}
