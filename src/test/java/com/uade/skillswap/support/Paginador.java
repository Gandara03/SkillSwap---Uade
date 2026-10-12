package com.uade.skillswap.support;

import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

import java.util.List;

/** Aplica un {@link PaginaRequest} real sobre una lista ya filtrada y ordenada. */
final class Paginador {

    private Paginador() { }

    static <T> Pagina<T> paginar(List<T> ordenados, PaginaRequest pedido) {
        int desde = (int) Math.min((long) pedido.offset(), ordenados.size());
        int hasta = Math.min(desde + pedido.tamano(), ordenados.size());
        return new Pagina<>(ordenados.subList(desde, hasta), pedido.pagina(), pedido.tamano(), ordenados.size());
    }
}
