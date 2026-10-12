package com.uade.skillswap.infrastructure.persistence.jpa.repository;

import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.ContratacionRepository;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.ContratacionJpaEntity;
import com.uade.skillswap.infrastructure.persistence.jpa.mapper.ContratacionMapper;
import jakarta.persistence.EntityNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Los eventos pendientes y los observers de la Contratacion viven solo en memoria: guardar() devuelve la MISMA
 * instancia (con id/version asignados) para que el servicio pueda notificarlos despues del commit.
 */
public class JpaContratacionRepository extends SimpleJpaRepository<ContratacionJpaEntity, Long>
        implements ContratacionRepository {

    public JpaContratacionRepository(JpaContexto contexto) {
        super(contexto, ContratacionJpaEntity.class);
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

    /** Contrataciones donde el perfil es cliente o proveedor, las mas recientes primero. Paginado en SQL + 1 fetch del historial. */
    @Override
    public Pagina<Contratacion> buscarPorParticipante(Long perfilId, PaginaRequest pagina) {
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("perfilId", perfilId);
            String desde = "from ContratacionJpaEntity c where c.clienteId = :perfilId or c.proveedorId = :perfilId";
            long total = contar(em, "select count(c) " + desde, parametros);
            if (total == 0 || pagina.offset() >= total) {
                return new Pagina<Contratacion>(List.of(), pagina.pagina(), pagina.tamano(), total);
            }
            List<Long> ids = idsPaginados(em, "select c.id " + desde + " order by c.creadaEn desc, c.id desc",
                    parametros, pagina);
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

    @Override
    public long contarPorPublicacion(Long publicacionId) {
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("publicacionId", publicacionId);
            return contar(em, "select count(c) from ContratacionJpaEntity c where c.publicacionId = :publicacionId",
                    parametros);
        });
    }
}
