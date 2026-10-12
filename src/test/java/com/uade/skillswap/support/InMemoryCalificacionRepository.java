package com.uade.skillswap.support;

import com.uade.skillswap.domain.model.calificacion.Calificacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.CalificacionRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Fake de {@link CalificacionRepository}. Emula el unico (contratacion, autor) del esquema real. */
public class InMemoryCalificacionRepository implements CalificacionRepository {

    private final List<Calificacion> calificaciones = new ArrayList<>();
    private long secuencia = 0;

    @Override
    public Calificacion guardar(Calificacion calificacion) {
        if (existe(calificacion.getContratacionId(), calificacion.getAutorId())) {
            throw new IllegalStateException("Ya existe una calificacion de ese autor para la contratacion "
                    + calificacion.getContratacionId());
        }
        if (calificacion.getId() == null) {
            Reflejo.asignar(calificacion, "id", ++secuencia);
        }
        calificaciones.add(calificacion);
        return calificacion;
    }

    @Override
    public boolean existe(Long contratacionId, Long autorId) {
        return calificaciones.stream()
                .anyMatch(c -> c.getContratacionId().equals(contratacionId) && c.getAutorId().equals(autorId));
    }

    @Override
    public List<Calificacion> buscarPorDestinatario(Long destinatarioId) {
        return calificaciones.stream()
                .filter(c -> c.getDestinatarioId().equals(destinatarioId))
                .sorted(masRecientePrimero())
                .collect(Collectors.toList());
    }

    @Override
    public Pagina<Calificacion> paginarPorDestinatario(Long destinatarioId, PaginaRequest pagina) {
        return Paginador.paginar(buscarPorDestinatario(destinatarioId), pagina);
    }

    private static Comparator<Calificacion> masRecientePrimero() {
        return Comparator.comparing(Calificacion::getFecha).reversed()
                .thenComparing(Calificacion::getId, Comparator.reverseOrder());
    }
}
