package com.uade.skillswap.application.service;

import com.uade.skillswap.application.command.BloqueDisponibilidadDto;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.domain.model.vo.BloqueDisponibilidad;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.domain.query.PaginaRequest;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Validacion y conversion de datos de entrada (comandos) antes de tocar el dominio.
 * Solo cubre lo que el dominio no valida (nulls, textos vacios, formatos); las reglas viven en el dominio.
 */
final class Entradas {

    private Entradas() { }

    static <T> T exigir(T valor, String nombre) {
        if (valor == null) {
            throw new ValidacionException(nombre + " es obligatorio");
        }
        return valor;
    }

    static void exigirTexto(String valor, String nombre) {
        if (valor == null || valor.isBlank()) {
            throw new ValidacionException(nombre + " es obligatorio");
        }
    }

    static void exigirPagina(PaginaRequest pagina) {
        exigir(pagina, "La pagina");
    }

    /** null o vacio significa "sin cambios / valor por defecto" (lo resuelve el dominio). */
    static ZoneId zona(String zona) {
        if (zona == null || zona.isBlank()) {
            return null;
        }
        try {
            return ZoneId.of(zona.trim());
        } catch (DateTimeException e) {
            throw new ValidacionException("Zona horaria invalida: " + zona);
        }
    }

    static SkillCoins precio(BigDecimal monto) {
        return monto == null ? null : SkillCoins.de(monto);
    }

    static List<Habilidad> habilidades(Collection<String> nombres) {
        List<Habilidad> resultado = new ArrayList<>();
        if (nombres != null) {
            for (String nombre : nombres) {
                exigirTexto(nombre, "El nombre de la habilidad");
                resultado.add(Habilidad.de(nombre));
            }
        }
        return resultado;
    }

    static Set<String> intereses(Collection<String> intereses) {
        Set<String> resultado = new LinkedHashSet<>();
        if (intereses != null) {
            for (String interes : intereses) {
                exigirTexto(interes, "El interes");
                resultado.add(interes.trim());
            }
        }
        return resultado;
    }

    static List<BloqueDisponibilidad> bloques(List<BloqueDisponibilidadDto> bloques) {
        List<BloqueDisponibilidad> resultado = new ArrayList<>();
        if (bloques != null) {
            for (BloqueDisponibilidadDto b : bloques) {
                exigir(b, "El bloque de disponibilidad");
                resultado.add(new BloqueDisponibilidad(b.dia(), b.desde(), b.hasta()));
            }
        }
        return resultado;
    }
}
