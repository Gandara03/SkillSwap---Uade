package com.uade.skillswap.domain.query;

import java.util.List;
import java.util.function.Function;

/** Resultado paginado. */
public record Pagina<T>(List<T> contenido, int pagina, int tamano, long totalElementos) {

    public Pagina {
        contenido = List.copyOf(contenido);
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
