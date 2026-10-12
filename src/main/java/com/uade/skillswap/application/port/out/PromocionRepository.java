package com.uade.skillswap.application.port.out;

import com.uade.skillswap.domain.model.promocion.Promocion;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface PromocionRepository {
    Promocion guardar(Promocion promocion);
    List<Promocion> buscarActivasPorPublicacion(Long publicacionId);

    /** Version por lote (una sola consulta) para listados: evita una consulta de promociones por fila (N+1). */
    Map<Long, List<Promocion>> buscarActivasPorPublicaciones(Collection<Long> publicacionIds);
}
