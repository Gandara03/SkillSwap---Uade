package com.uade.skillswap.domain.query;

import java.util.List;
import java.util.function.Function;

/** Resultado paginado. */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos) {

    public Pagina {
        contenido = List.copyOf(contenido);
    }

    /**
     * Pagina de una lista que ya esta completa y ordenada en memoria (por ejemplo, un ranking o el historial de un
     * agregado). Si el pedido pasa del final, devuelve una pagina vacia que igual informa el total.
     */
    public static <T> Pagina<T> recortar(List<T> ordenados, PaginaRequest pedido) {
        int desde = (int) Math.min((long) pedido.offset(), ordenados.size());
        int hasta = Math.min(desde + pedido.tamano(), ordenados.size());
        return new Pagina<>(ordenados.subList(desde, hasta), pedido.pagina(), pedido.tamano(), ordenados.size());
    }

    public int totalPaginas() {
        return tamano == 0 ? 0 : (int) Math.ceil((double) totalElementos / tamano);
    }

    public boolean tieneSiguiente() {
        return pagina + 1 < totalPaginas();
    }

    public <R> Pagina<R> mapear(Function<T, R> f) {
        return new Pagina<>(contenido.stream().map(f).toList(), pagina, tamano, totalElementos);
    }
}
