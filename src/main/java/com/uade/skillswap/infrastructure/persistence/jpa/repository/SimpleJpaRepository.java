package com.uade.skillswap.infrastructure.persistence.jpa.repository;

import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Base de los repositorios JPA, al estilo del SimpleJpaRepository del curso (MagicCafe) pero sin JPAUtil estatico:
 * trabaja sobre un {@link JpaContexto}, asi que participa de la transaccion del caso de uso si hay una abierta.
 * T es la entidad JPA (no el agregado de dominio). Ademas del CRUD generico trae los helpers de consulta que
 * comparten los repositorios: parametros nombrados, conteo, paginacion por ids y precarga de colecciones.
 */
public abstract class SimpleJpaRepository<T, ID> {

    protected final JpaContexto contexto;
    private final Class<T> entidad;

    protected SimpleJpaRepository(JpaContexto contexto, Class<T> entidad) {
        this.contexto = contexto;
        this.entidad = entidad;
    }

    // ---------- CRUD generico sobre la entidad ----------

    /** Persiste o actualiza la entidad (merge). Devuelve la instancia gestionada. */
    public T guardarEntidad(T e) {
        return escribir(em -> em.merge(e));
    }

    /** Ojo: la entidad vuelve desconectada; sus colecciones lazy no se pueden recorrer fuera de la unidad. */
    public Optional<T> buscarEntidad(ID id) {
        return leer(em -> Optional.ofNullable(em.find(entidad, id)));
    }

    public List<T> listarEntidades() {
        return leer(em -> em.createQuery("select e from " + entidad.getSimpleName() + " e", entidad).getResultList());
    }

    public void eliminarEntidadPorId(ID id) {
        escribir(em -> {
            T e = em.find(entidad, id);
            if (e != null) {
                em.remove(e);
            }
            return null;
        });
    }

    // ---------- acceso al contexto ----------

    protected <R> R leer(Function<EntityManager, R> trabajo) {
        return contexto.leer(trabajo);
    }

    protected <R> R escribir(Function<EntityManager, R> trabajo) {
        return contexto.escribir(trabajo);
    }

    // ---------- concurrencia ----------

    /**
     * Optimistic locking entre lecturas y escrituras separadas: si el agregado que se guarda fue leido con una
     * version distinta de la que hay en la base, alguien lo modifico en el medio. Hibernate (@Version) cubre el
     * resto: la carrera entre este chequeo y el commit.
     */
    protected static void exigirVersion(String agregado, Object id, Long versionDominio, Long versionPersistida) {
        if (versionDominio != null && !versionDominio.equals(versionPersistida)) {
            throw new OptimisticLockException(agregado + " " + id + " fue modificado por otro usuario (version leida "
                    + versionDominio + ", version actual " + versionPersistida + ")");
        }
    }

    // ---------- consultas ----------

    /** Crea una consulta con parametros nombrados (nunca se concatenan valores en el JPQL). */
    protected <R> TypedQuery<R> consulta(EntityManager em, String jpql, Class<R> tipo, Map<String, Object> parametros) {
        TypedQuery<R> q = em.createQuery(jpql, tipo);
        for (Map.Entry<String, Object> p : parametros.entrySet()) {
            q.setParameter(p.getKey(), p.getValue());
        }
        return q;
    }

    protected <R> List<R> listar(EntityManager em, String jpql, Class<R> tipo, Map<String, Object> parametros) {
        return consulta(em, jpql, tipo, parametros).getResultList();
    }

    protected long contar(EntityManager em, String jpqlCount, Map<String, Object> parametros) {
        return consulta(em, jpqlCount, Long.class, parametros).getSingleResult();
    }

    /** Primera pagina de ids de una consulta {@code select x.id ...} ya ordenada (sin tocar colecciones: paginacion en SQL). */
    protected List<Long> idsPaginados(EntityManager em, String jpql, Map<String, Object> parametros, PaginaRequest pagina) {
        return consulta(em, jpql, Long.class, parametros)
                .setFirstResult(pagina.offset())
                .setMaxResults(pagina.tamano())
                .getResultList();
    }

    /**
     * Inicializa colecciones lazy de las entidades con esos ids usando UNA consulta por coleccion
     * ({@code left join fetch ... where id in :ids}), en vez de una por entidad (N+1). Se hace aparte de la consulta
     * paginada porque combinar JOIN FETCH de colecciones con setMaxResults obliga a paginar en memoria.
     */
    protected <E> void precargar(EntityManager em, Class<E> clase, Collection<Long> ids, String... colecciones) {
        if (ids.isEmpty()) {
            return;
        }
        for (String coleccion : colecciones) {
            em.createQuery("select distinct e from " + clase.getSimpleName() + " e left join fetch e." + coleccion
                            + " where e.id in :ids", clase)
                    .setParameter("ids", ids)
                    .getResultList();
        }
    }

    /** Carga las entidades por ids (una consulta) y las devuelve en el orden de {@code ids}. */
    protected <E> List<E> cargarEnOrden(EntityManager em, Class<E> clase, List<Long> ids, Function<E, Long> idDe) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("ids", ids);
        List<E> cargadas = listar(em, "select e from " + clase.getSimpleName() + " e where e.id in :ids", clase, parametros);
        Map<Long, E> porId = new HashMap<>();
        for (E e : cargadas) {
            porId.put(idDe.apply(e), e);
        }
        List<E> ordenadas = new ArrayList<>(ids.size());
        for (Long id : ids) {
            ordenadas.add(porId.get(id));
        }
        return ordenadas;
    }

    /** Escapa los comodines de LIKE usando '!' como caracter de escape (hay que declarar {@code escape '!'}). */
    protected static String escaparLike(String texto) {
        return texto.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }
}
