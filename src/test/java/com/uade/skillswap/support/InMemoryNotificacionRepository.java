package com.uade.skillswap.support;

import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.NotificacionRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Fake de {@link NotificacionRepository}: bandeja ordenada de la mas reciente a la mas antigua. */
public class InMemoryNotificacionRepository implements NotificacionRepository {

    private final Map<Long, Notificacion> notificaciones = new LinkedHashMap<>();
    private long secuencia = 0;

    @Override
    public Notificacion guardar(Notificacion notificacion) {
        if (notificacion.getId() == null) {
            Reflejo.asignar(notificacion, "id", ++secuencia);
        }
        notificaciones.put(notificacion.getId(), notificacion);
        return notificacion;
    }

    @Override
    public Optional<Notificacion> buscarPorId(Long id) {
        return Optional.ofNullable(notificaciones.get(id));
    }

    @Override
    public Pagina<Notificacion> buscarPorDestinatario(Long destinatarioId, boolean soloNoLeidas, PaginaRequest pagina) {
        List<Notificacion> bandeja = notificaciones.values().stream()
                .filter(n -> n.getDestinatarioId().equals(destinatarioId))
                .filter(n -> !soloNoLeidas || !n.estaLeida())
                .sorted(Comparator.comparing(Notificacion::getFecha).reversed()
                        .thenComparing(Notificacion::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());
        return Paginador.paginar(bandeja, pagina);
    }

    /** Todas las guardadas, en orden de creacion (para asserts de tests). */
    public List<Notificacion> todas() {
        return new ArrayList<>(notificaciones.values());
    }
}
