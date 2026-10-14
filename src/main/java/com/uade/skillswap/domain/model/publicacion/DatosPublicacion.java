package com.uade.skillswap.domain.model.publicacion;

import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.util.Collection;

/**
 * Objeto de parametros con los datos editables de una publicacion. Agrupa lo que crearBorrador y editar
 * recibian como diez parametros sueltos (bad smell "Long Parameter List"): si se agrega un dato, cambia este
 * record y no la firma de esos metodos. No tiene reglas propias; la completitud la valida Publicacion al publicar.
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
