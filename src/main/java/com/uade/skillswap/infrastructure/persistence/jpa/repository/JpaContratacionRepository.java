package com.uade.skillswap.infrastructure.persistence.jpa.repository;

import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.ContratacionRepository;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.ContratacionJpaEntity;
import com.uade.skillswap.infrastructure.persistence.jpa.mapper.ContratacionMapper;
import jakarta.persistence.EntityNotFoundException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Los eventos pendientes y los observers de la Contratacion viven solo en memoria: guardar() devuelve la MISMA
 * instancia (con id/version asignados) para que el servicio pueda notificarlos despues del commit.
 */
public class JpaContratacionRepository extends SimpleJpaRepository
        implements ContratacionRepository {

    private static final String COMPLETADA = "COMPLETADA";

    public JpaContratacionRepository(JpaContexto contexto) {
        super(contexto);
    }

    @Override
    public Contratacion guardar(Contratacion contratacion) {
        return escribir(em -> {
            ContratacionJpaEntity e;
            if (contratacion.getId() == null) {
                e = new ContratacionJpaEntity();
                ContratacionMapper.aplicar(contratacion, e);
                em.persist(e);
            } else {
                e = em.find(ContratacionJpaEntity.class, contratacion.getId());
                if (e == null) {
                    throw new EntityNotFoundException("No existe la contratacion " + contratacion.getId());
                }
                exigirVersion("La contratacion", contratacion.getId(), contratacion.getVersion(), e.getVersion());
                ContratacionMapper.aplicar(contratacion, e);
            }
            em.flush();
            ContratacionMapper.confirmar(contratacion, e);
            return contratacion;
        });
    }

    @Override
    public Optional<Contratacion> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return leer(em -> Optional.ofNullable(em.find(ContratacionJpaEntity.class, id)).map(ContratacionMapper::aDominio));
    }

    /** Contrataciones donde el perfil es cliente o proveedor, las mas recientes primero. */
    @Override
    public Pagina<Contratacion> buscarPorParticipante(Long perfilId, PaginaRequest pagina) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("perfilId", perfilId);
        return paginar("from ContratacionJpaEntity c where c.clienteId = :perfilId or c.proveedorId = :perfilId",
                "c.creadaEn desc, c.id desc", parametros, pagina);
    }

    @Override
    public Pagina<Contratacion> buscarCompletadasPorProveedor(Long proveedorId, PaginaRequest pagina) {
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("proveedorId", proveedorId);
        parametros.put("estado", COMPLETADA);
        return paginar("from ContratacionJpaEntity c where c.proveedorId = :proveedorId and c.estado = :estado",
                "c.completadaEn desc, c.id desc", parametros, pagina);
    }

    /** Paginado en SQL (count + ids de la pagina) y despues las entidades con 1 fetch del historial. */
    private Pagina<Contratacion> paginar(String desde, String orden, Map<String, Object> parametros,
                                         PaginaRequest pagina) {
        return leer(em -> {
            long total = contar(em, "select count(c) " + desde, parametros);
            if (total == 0 || pagina.offset() >= total) {
                return new Pagina<Contratacion>(List.of(), pagina.pagina(), pagina.tamano(), total);
            }
            List<Long> ids = idsPaginados(em, "select c.id " + desde + " order by " + orden, parametros, pagina);
            List<ContratacionJpaEntity> entidades = cargarEnOrden(em, ContratacionJpaEntity.class, ids,
                    ContratacionJpaEntity::getId);
            precargar(em, ContratacionJpaEntity.class, ids, "historial");
            List<Contratacion> resultado = new ArrayList<>(entidades.size());
            for (ContratacionJpaEntity e : entidades) {
                resultado.add(ContratacionMapper.aDominio(e));
            }
            return new Pagina<>(resultado, pagina.pagina(), pagina.tamano(), total);
        });
    }

    /** Un {@code count} agrupado por publicacion: una sola sentencia para todas las candidatas. */
    @Override
    public Map<Long, Long> contarPagadasDesde(Collection<Long> publicacionIds, LocalDateTime desde) {
        if (publicacionIds == null || publicacionIds.isEmpty()) {
            return Map.of();
        }
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("ids", publicacionIds);
            parametros.put("desde", desde);
            List<Object[]> filas = listar(em, "select c.publicacionId, count(c) from ContratacionJpaEntity c"
                    + " where c.publicacionId in :ids and c.pagadaEn >= :desde group by c.publicacionId",
                    Object[].class, parametros);
            Map<Long, Long> cantidades = new HashMap<>();
            for (Object[] fila : filas) {
                cantidades.put((Long) fila[0], (Long) fila[1]);
            }
            return cantidades;
        });
    }
}
