package com.uade.skillswap.domain.model.publicacion.estado;

import com.uade.skillswap.domain.model.historial.ContextoTransicion;
import com.uade.skillswap.domain.model.publicacion.Publicacion;

/** BORRADOR: editable y no visible. */
public class EstadoBorrador extends EstadoPublicacionBase {

    @Override
    public String getNombre() {
        return "BORRADOR";
    }

    @Override
    public void publicar(Publicacion p, ContextoTransicion t) {
        p.validarCompletaParaPublicar();
        p.cambiarEstado(new EstadoPublicada(), t);
        p.marcarPublicada(t.getFecha());
    }

    @Override
    public void cancelar(Publicacion p, ContextoTransicion t) {
        p.cambiarEstado(new EstadoCancelada(), t.exigirMotivo());
    }

    @Override
    public boolean permiteEdicion() {
        return true;
    }
}
