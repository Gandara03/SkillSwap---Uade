package com.uade.skillswap.support;

import com.uade.skillswap.domain.model.promocion.Promocion;
import com.uade.skillswap.application.port.out.PromocionRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

/** Fake de {@link PromocionRepository}: devuelve las promociones activas (la vigencia horaria la evalua el dominio). */
public class InMemoryPromocionRepository implements PromocionRepository {

    private final List<Promocion> promociones = new ArrayList<>();
    private long secuencia = 0;

    @Override
    public Promocion guardar(Promocion promocion) {
        if (promocion.getId() == null) {
            Reflejo.asignar(promocion, "id", ++secuencia);
            promociones.add(promocion);
        }
        return promocion;
    }

    @Override
    public List<Promocion> buscarActivasPorPublicacion(Long publicacionId) {
        return promociones.stream()
                .filter(p -> p.getPublicacionId().equals(publicacionId) && p.isActiva())
                .collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<Promocion>> buscarActivasPorPublicaciones(Collection<Long> publicacionIds) {
        Map<Long, List<Promocion>> resultado = new LinkedHashMap<>();
        for (Promocion p : promociones) {
            if (p.isActiva() && publicacionIds.contains(p.getPublicacionId())) {
                resultado.computeIfAbsent(p.getPublicacionId(), k -> new ArrayList<>()).add(p);
            }
        }
        return resultado;
    }
}
