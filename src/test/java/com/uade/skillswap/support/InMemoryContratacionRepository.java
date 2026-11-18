package com.uade.skillswap.support;

import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.ContratacionRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Fake de {@link ContratacionRepository}. Guarda referencias (la contratacion conserva sus eventos
 * pendientes y observers, como en el flujo real despues del commit).
 */
public class InMemoryContratacionRepository implements ContratacionRepository {

    private final Map<Long, Contratacion> contrataciones = new LinkedHashMap<>();
    private long secuencia = 0;

    @Override
    public Contratacion guardar(Contratacion contratacion) {
        if (contratacion.getId() == null) {
            Reflejo.asignar(contratacion, "id", ++secuencia);
        }
        Reflejo.asignar(contratacion, "version", Reflejo.version(contratacion.getVersion()));
        contrataciones.put(contratacion.getId(), contratacion);
        return contratacion;
    }

    @Override
    public Optional<Contratacion> buscarPorId(Long id) {
        return Optional.ofNullable(contrataciones.get(id));
    }

    @Override
    public Pagina<Contratacion> buscarPorParticipante(Long perfilId, PaginaRequest pagina) {
        List<Contratacion> propias = contrataciones.values().stream()
                .filter(c -> c.esParticipante(perfilId))
                .sorted(Comparator.comparing(Contratacion::getCreadaEn).reversed()
                        .thenComparing(Contratacion::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());
        return Paginador.paginar(propias, pagina);
    }

    @Override
    public Pagina<Contratacion> buscarCompletadasPorProveedor(Long proveedorId, PaginaRequest pagina) {
        List<Contratacion> completadas = contrataciones.values().stream()
                .filter(c -> proveedorId.equals(c.getProveedorId()) && c.estaCompletada())
                .sorted(Comparator.comparing(Contratacion::getCompletadaEn).reversed()
                        .thenComparing(Contratacion::getId, Comparator.reverseOrder()))
                .collect(Collectors.toList());
        return Paginador.paginar(completadas, pagina);
    }

    /** Mismo criterio que la consulta agrupada de JPA: pagadaEn desde esa fecha, inclusive. */
    @Override
    public Map<Long, Long> contarPagadasDesde(Collection<Long> publicacionIds, LocalDateTime desde) {
        if (publicacionIds == null || publicacionIds.isEmpty()) {
            return Map.of();
        }
        return contrataciones.values().stream()
                .filter(c -> publicacionIds.contains(c.getPublicacionId()))
                .filter(c -> c.getPagadaEn() != null && !c.getPagadaEn().isBefore(desde))
                .collect(Collectors.groupingBy(Contratacion::getPublicacionId, Collectors.counting()));
    }
}
