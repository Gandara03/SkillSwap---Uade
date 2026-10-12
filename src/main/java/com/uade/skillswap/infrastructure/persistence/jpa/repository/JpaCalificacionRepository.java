package com.uade.skillswap.infrastructure.persistence.jpa.repository;

import com.uade.skillswap.domain.model.calificacion.Calificacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.CalificacionRepository;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.CalificacionJpaEntity;
import com.uade.skillswap.infrastructure.persistence.jpa.mapper.CalificacionMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Las calificaciones son inmutables: guardar siempre inserta. El unico (contratacion, autor) lo impone la base. */
public class JpaCalificacionRepository extends SimpleJpaRepository<CalificacionJpaEntity, Long>
        implements CalificacionRepository {

    private static final String MAS_RECIENTE = " order by c.fecha desc, c.id desc";

    public JpaCalificacionRepository(JpaContexto contexto) {
        super(contexto, CalificacionJpaEntity.class);
    }

    @Override
    public Calificacion guardar(Calificacion calificacion) {
        if (calificacion.getId() != null) {
            return calificacion;
        }
        return escribir(em -> {
            CalificacionJpaEntity e = CalificacionMapper.aEntidad(calificacion);
            em.persist(e);
            em.flush();
            CalificacionMapper.confirmar(calificacion, e);
            return calificacion;
        });
    }

    @Override
    public boolean existe(Long contratacionId, Long autorId) {
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("contratacionId", contratacionId);
            parametros.put("autorId", autorId);
            return contar(em, "select count(c) from CalificacionJpaEntity c "
                    + "where c.contratacionId = :contratacionId and c.autorId = :autorId", parametros) > 0;
        });
    }

    @Override
    public List<Calificacion> buscarPorDestinatario(Long destinatarioId) {
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("destinatarioId", destinatarioId);
            return aDominio(listar(em, "select c from CalificacionJpaEntity c where c.destinatarioId = :destinatarioId"
                    + MAS_RECIENTE, CalificacionJpaEntity.class, parametros));
        });
    }

    @Override
    public Pagina<Calificacion> paginarPorDestinatario(Long destinatarioId, PaginaRequest pagina) {
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("destinatarioId", destinatarioId);
            String desde = "from CalificacionJpaEntity c where c.destinatarioId = :destinatarioId";
            long total = contar(em, "select count(c) " + desde, parametros);
            if (total == 0 || pagina.offset() >= total) {
                return new Pagina<Calificacion>(List.of(), pagina.pagina(), pagina.tamano(), total);
            }
            List<CalificacionJpaEntity> filas = consulta(em, "select c " + desde + MAS_RECIENTE,
                    CalificacionJpaEntity.class, parametros)
                    .setFirstResult(pagina.offset())
                    .setMaxResults(pagina.tamano())
                    .getResultList();
            return new Pagina<>(aDominio(filas), pagina.pagina(), pagina.tamano(), total);
        });
    }

    private static List<Calificacion> aDominio(List<CalificacionJpaEntity> filas) {
        List<Calificacion> resultado = new ArrayList<>(filas.size());
        for (CalificacionJpaEntity e : filas) {
            resultado.add(CalificacionMapper.aDominio(e));
        }
        return resultado;
    }
}
