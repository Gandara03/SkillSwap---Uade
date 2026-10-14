package com.uade.skillswap.infrastructure.persistence.jpa.repository;

import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.domain.model.vo.RangoPrecio;
import com.uade.skillswap.domain.query.CriterioOrden;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.PublicacionRepository;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.PublicacionJpaEntity;
import com.uade.skillswap.infrastructure.persistence.jpa.mapper.PublicacionMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class JpaPublicacionRepository extends SimpleJpaRepository
        implements PublicacionRepository {

    private static final String PUBLICADA = "PUBLICADA";

    /** Mas reciente = fecha de publicacion (o de creacion si todavia no se publico), desempatando por id. */
    private static final String MAS_RECIENTE = "coalesce(p.publicadaEn, p.creadaEn) desc, p.id desc";

    public JpaPublicacionRepository(JpaContexto contexto) {
        super(contexto);
    }

    @Override
    public Publicacion guardar(Publicacion publicacion) {
        return escribir(em -> {
            PublicacionJpaEntity e;
            if (publicacion.getId() == null) {
                e = new PublicacionJpaEntity();
                PublicacionMapper.aplicar(publicacion, e);
                em.persist(e);
            } else {
                e = em.find(PublicacionJpaEntity.class, publicacion.getId());
                if (e == null) {
                    throw new EntityNotFoundException("No existe la publicacion " + publicacion.getId());
                }
                exigirVersion("La publicacion", publicacion.getId(), publicacion.getVersion(), e.getVersion());
                PublicacionMapper.aplicar(publicacion, e);
            }
            em.flush();
            PublicacionMapper.confirmar(publicacion, e);
            return publicacion;
        });
    }

    @Override
    public Optional<Publicacion> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return leer(em -> Optional.ofNullable(em.find(PublicacionJpaEntity.class, id)).map(PublicacionMapper::aDominio));
    }

    // ---------- busqueda ----------

    /**
     * Busqueda dinamica de publicaciones PUBLICADAS. El JPQL se arma por fragmentos fijos y todos los valores
     * viajan como parametros nombrados. Estrategia contra N+1 y paginacion en memoria:
     * (1) count, (2) ids de la pagina ordenados (paginacion en SQL, sin JOIN FETCH de colecciones),
     * (3) carga de esas entidades y una consulta por coleccion con JOIN FETCH (4 en total), en el orden de los ids.
     * Los filtros sobre colecciones (habilidad, etiqueta, franjas) usan subconsultas "id in (...)" para no
     * multiplicar filas ni romper el count.
     */
    @Override
    public Pagina<Publicacion> buscar(FiltroBusqueda filtro, PaginaRequest pagina) {
        FiltroBusqueda f = filtro == null ? FiltroBusqueda.vacio() : filtro;
        return leer(em -> {
            Condiciones c = condiciones(f);
            String desde = "from PublicacionJpaEntity p join p.autor a where " + c.where();
            long total = contar(em, "select count(p) " + desde, c.parametros());
            if (total == 0 || pagina.offset() >= total) {
                return new Pagina<Publicacion>(List.of(), pagina.pagina(), pagina.tamano(), total);
            }
            List<Long> ids = idsPaginados(em, "select p.id " + desde + " order by " + orden(f.ordenEfectivo(), hayTexto(f.texto())),
                    c.parametros(), pagina);
            return new Pagina<>(cargar(em, ids), pagina.pagina(), pagina.tamano(), total);
        });
    }

    @Override
    public Pagina<Publicacion> buscarPorAutor(Long autorId, PaginaRequest pagina) {
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("autorId", autorId);
            String desde = "from PublicacionJpaEntity p where p.autorId = :autorId";
            long total = contar(em, "select count(p) " + desde, parametros);
            if (total == 0 || pagina.offset() >= total) {
                return new Pagina<Publicacion>(List.of(), pagina.pagina(), pagina.tamano(), total);
            }
            List<Long> ids = idsPaginados(em, "select p.id " + desde + " order by " + MAS_RECIENTE, parametros, pagina);
            return new Pagina<>(cargar(em, ids), pagina.pagina(), pagina.tamano(), total);
        });
    }

    @Override
    public List<Publicacion> buscarPublicadas(int limite) {
        if (limite <= 0) {
            return List.of();
        }
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("estado", PUBLICADA);
            List<Long> ids = consulta(em, "select p.id from PublicacionJpaEntity p where p.estado = :estado order by "
                    + MAS_RECIENTE, Long.class, parametros).setMaxResults(limite).getResultList();
            return cargar(em, ids);
        });
    }

    /** Carga las publicaciones de la pagina con todas sus colecciones, manteniendo el orden de {@code ids}. */
    private List<Publicacion> cargar(EntityManager em, List<Long> ids) {
        List<PublicacionJpaEntity> entidades = cargarEnOrden(em, PublicacionJpaEntity.class, ids,
                PublicacionJpaEntity::getId);
        precargar(em, PublicacionJpaEntity.class, ids, "franjas", "etiquetas", "habilidades", "historial");
        List<Publicacion> resultado = new ArrayList<>(entidades.size());
        for (PublicacionJpaEntity e : entidades) {
            resultado.add(PublicacionMapper.aDominio(e));
        }
        return resultado;
    }

    private static String orden(CriterioOrden criterio, boolean hayTexto) {
        return switch (criterio) {
            case RELEVANCIA -> (hayTexto ? "case when lower(p.titulo) like :texto escape '!' then 0 else 1 end asc, " : "")
                    + "a.reputacion desc, " + MAS_RECIENTE;
            case RECIENTES -> MAS_RECIENTE;
            case PRECIO_ASC -> "p.precioBase asc, " + MAS_RECIENTE;
            case PRECIO_DESC -> "p.precioBase desc, " + MAS_RECIENTE;
            case REPUTACION -> "a.reputacion desc, " + MAS_RECIENTE;
        };
    }

    // ---------- armado del WHERE ----------

    private static Condiciones condiciones(FiltroBusqueda f) {
        Condiciones c = new Condiciones();
        c.agregar("p.estado = :estadoPublicada", "estadoPublicada", PUBLICADA);

        if (hayTexto(f.texto())) {
            String patron = "%" + escaparLike(f.texto().trim().toLowerCase(Locale.ROOT)) + "%";
            c.agregar("(lower(p.titulo) like :texto escape '!' or lower(p.descripcion) like :texto escape '!')",
                    "texto", patron);
        }
        if (hayTexto(f.categoria())) {
            c.agregar("lower(p.categoria) = :categoria", "categoria", f.categoria().trim().toLowerCase(Locale.ROOT));
        }
        if (f.tipo() != null) {
            c.agregar("p.tipo = :tipo", "tipo", f.tipo().name());
        }
        if (f.modalidad() != null) {
            c.agregar("p.modalidad = :modalidad", "modalidad", f.modalidad().name());
        }
        RangoPrecio rango = f.rangoPrecio();
        if (rango != null && rango.getMinimo() != null) {
            c.agregar("p.precioBase >= :precioMinimo", "precioMinimo", rango.getMinimo().getMonto());
        }
        if (rango != null && rango.getMaximo() != null) {
            c.agregar("p.precioBase <= :precioMaximo", "precioMaximo", rango.getMaximo().getMonto());
        }
        if (hayTexto(f.habilidad())) {
            c.agregar("p.id in (select ph.id from PublicacionJpaEntity ph join ph.habilidades h where h = :habilidad)",
                    "habilidad", Habilidad.de(f.habilidad()).getNombre());
        }
        if (hayTexto(f.etiqueta())) {
            c.agregar("p.id in (select pe.id from PublicacionJpaEntity pe join pe.etiquetas t where lower(t) = :etiqueta)",
                    "etiqueta", f.etiqueta().trim().toLowerCase(Locale.ROOT));
        }
        if (f.reputacionMinima() != null) {
            c.agregar("a.reputacion >= :reputacionMinima", "reputacionMinima", f.reputacionMinima());
        }
        if (f.disponibleDesde() != null || f.disponibleHasta() != null) {
            // Existe una franja LIBRE completamente dentro del rango pedido (cualquiera de los extremos puede faltar).
            StringBuilder franja = new StringBuilder(
                    "p.id in (select fr.publicacion.id from FranjaJpaEntity fr where fr.ocupada = false");
            if (f.disponibleDesde() != null) {
                franja.append(" and fr.inicio >= :disponibleDesde");
                c.parametro("disponibleDesde", f.disponibleDesde());
            }
            if (f.disponibleHasta() != null) {
                franja.append(" and fr.fin <= :disponibleHasta");
                c.parametro("disponibleHasta", f.disponibleHasta());
            }
            c.condicion(franja.append(")").toString());
        }
        return c;
    }

    private static boolean hayTexto(String s) {
        return s != null && !s.isBlank();
    }

    /** Acumula condiciones AND y sus parametros nombrados. */
    private static final class Condiciones {
        private final StringBuilder where = new StringBuilder();
        private final Map<String, Object> parametros = new HashMap<>();

        void condicion(String jpql) {
            if (where.length() > 0) {
                where.append(" and ");
            }
            where.append(jpql);
        }

        void parametro(String nombre, Object valor) {
            parametros.put(nombre, valor);
        }

        void agregar(String jpql, String nombre, Object valor) {
            condicion(jpql);
            parametro(nombre, valor);
        }

        String where() {
            return where.toString();
        }

        Map<String, Object> parametros() {
            return parametros;
        }
    }
}
