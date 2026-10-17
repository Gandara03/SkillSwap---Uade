package com.uade.skillswap.domain.model.publicacion;

import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.util.Collection;

/**
 * Datos editables de una publicacion: objeto de parametros de crearBorrador y editar. No tiene reglas propias;
 * la completitud la valida Publicacion al publicar.
 */
public record DatosPublicacion(
        String titulo,
        String descripcion,
        String categoria,
        Modalidad modalidad,
        int duracionMinutos,
        int cupo,
        SkillCoins precioBase,
        Collection<String> etiquetas,
        Collection<Habilidad> habilidades,
        TipoPoliticaCancelacion politicaCancelacion) { }
