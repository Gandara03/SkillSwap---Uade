package com.uade.skillswap.support;

import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.publicacion.Franja;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.domain.query.CriterioOrden;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.PerfilRepository;
import com.uade.skillswap.application.port.out.PublicacionRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Fake de {@link PublicacionRepository} con filtros, orden y paginacion reales.
 * Recibe el repositorio de perfiles para resolver la reputacion del autor (filtro y orden REPUTACION).
 * Disponibilidad: una franja LIBRE cuyo inicio no sea anterior a {@code disponibleDesde} y cuyo fin
 * no sea posterior a {@code disponibleHasta}.
 */
public class InMemoryPublicacionRepository implements PublicacionRepository {

    private final Map<Long, Publicacion> publicaciones = new LinkedHashMap<>();
    private final PerfilRepository perfiles;
    private long secuencia = 0;
    private long secuenciaFranjas = 0;

    public InMemoryPublicacionRepository(PerfilRepository perfiles) {
        this.perfiles = perfiles;
    }

    @Override
    public Publicacion guardar(Publicacion publicacion) {
        if (publicacion.getId() == null) {
            Reflejo.asignar(publicacion, "id", ++secuencia);
        }
        for (Franja f : publicacion.getFranjas()) {
            if (f.getId() == null) {
                Reflejo.asignar(f, "id", ++secuenciaFranjas);
            }
        }
        Reflejo.asignar(publicacion, "version", Reflejo.version(publicacion.getVersion()));
        publicaciones.put(publicacion.getId(), publicacion);
        return publicacion;
    }

    @Override
    public Optional<Publicacion> buscarPorId(Long id) {
        return Optional.ofNullable(publicaciones.get(id));
    }

    @Override
    public Pagina<Publicacion> buscar(FiltroBusqueda filtro, PaginaRequest pagina) {
        List<Publicacion> ordenadas = publicaciones.values().stream()
                .filter(Publicacion::estaPublicada)
                .filter(p -> cumple(p, filtro))
                .sorted(comparador(filtro.ordenEfectivo(), filtro.texto()))
                .collect(Collectors.toList());
        return Paginador.paginar(ordenadas, pagina);
    }

    @Override
    public Pagina<Publicacion> buscarPorAutor(Long autorId, PaginaRequest pagina) {
        List<Publicacion> propias = publicaciones.values().stream()
                .filter(p -> p.esAutor(autorId))
                .sorted(masRecientePrimero())
                .collect(Collectors.toList());
        return Paginador.paginar(propias, pagina);
    }

    @Override
    public List<Publicacion> buscarPublicadas(int limite) {
        return publicaciones.values().stream()
                .filter(Publicacion::estaPublicada)
                .sorted(masRecientePrimero())
                .limit(Math.max(limite, 0))
                .collect(Collectors.toList());
    }

    public int cantidad() {
        return publicaciones.size();
    }

    // ---------- filtros ----------

    private boolean cumple(Publicacion p, FiltroBusqueda f) {
        return coincideTexto(p, f.texto())
                && (f.categoria() == null || f.categoria().equalsIgnoreCase(p.getCategoria()))
                && (f.tipo() == null || f.tipo() == p.getTipo())
                && (f.modalidad() == null || f.modalidad() == p.getModalidad())
                && (f.rangoPrecio() == null || f.rangoPrecio().incluye(p.getPrecioBase()))
                && (f.habilidad() == null || tieneHabilidad(p, f.habilidad()))
                && (f.etiqueta() == null || tieneEtiqueta(p, f.etiqueta()))
                && (f.reputacionMinima() == null || reputacionDelAutor(p) >= f.reputacionMinima())
                && tieneFranjaLibreEnVentana(p, f.disponibleDesde(), f.disponibleHasta());
    }

    private static boolean coincideTexto(Publicacion p, String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        String buscado = texto.trim().toLowerCase(Locale.ROOT);
        return contiene(p.getTitulo(), buscado) || contiene(p.getDescripcion(), buscado);
    }

    private static boolean contiene(String campo, String buscado) {
        return campo != null && campo.toLowerCase(Locale.ROOT).contains(buscado);
    }

    private static boolean tieneHabilidad(Publicacion p, String habilidad) {
        return p.getHabilidades().contains(Habilidad.de(habilidad));
    }

    private static boolean tieneEtiqueta(Publicacion p, String etiqueta) {
        return p.getEtiquetas().stream().anyMatch(e -> e.equalsIgnoreCase(etiqueta.trim()));
    }

    private static boolean tieneFranjaLibreEnVentana(Publicacion p, LocalDateTime desde, LocalDateTime hasta) {
        if (desde == null && hasta == null) {
            return true;
        }
        return p.getFranjas().stream()
                .filter(fr -> !fr.estaOcupada())
                .anyMatch(fr -> (desde == null || !fr.getInicio().isBefore(desde))
                        && (hasta == null || !fr.getFin().isAfter(hasta)));
    }

    private double reputacionDelAutor(Publicacion p) {
        return perfiles.buscarPorId(p.getAutorId()).map(Perfil::getReputacion).orElse(0.0);
    }

    // ---------- orden ----------

    private Comparator<Publicacion> comparador(CriterioOrden orden, String texto) {
        return switch (orden) {
            case RELEVANCIA -> {
                String t = texto == null ? "" : texto.trim().toLowerCase();
                Comparator<Publicacion> porTitulo = Comparator.comparingInt(
                        p -> !t.isEmpty() && p.getTitulo() != null && p.getTitulo().toLowerCase().contains(t) ? 0 : 1);
                yield porTitulo.thenComparing(Comparator.<Publicacion>comparingDouble(this::reputacionDelAutor).reversed())
                        .thenComparing(masRecientePrimero());
            }
            case RECIENTES -> masRecientePrimero();
            case PRECIO_ASC -> Comparator.comparing(Publicacion::getPrecioBase).thenComparing(masRecientePrimero());
            case PRECIO_DESC -> Comparator.comparing(Publicacion::getPrecioBase).reversed()
                    .thenComparing(masRecientePrimero());
            case REPUTACION -> Comparator.<Publicacion>comparingDouble(this::reputacionDelAutor).reversed()
                    .thenComparing(masRecientePrimero());
        };
    }

    private static Comparator<Publicacion> masRecientePrimero() {
        return Comparator.comparing((Publicacion p) -> p.getPublicadaEn() != null ? p.getPublicadaEn() : p.getCreadaEn(),
                        Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder())).reversed()
                .thenComparing(Publicacion::getId, Comparator.nullsLast(Comparator.<Long>reverseOrder()));
    }
}
