package com.uade.skillswap.infrastructure.persistence.jpa.repository;

import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.NotificacionRepository;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.NotificacionJpaEntity;
import com.uade.skillswap.infrastructure.persistence.jpa.mapper.NotificacionMapper;
import jakarta.persistence.EntityNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JpaNotificacionRepository extends SimpleJpaRepository<NotificacionJpaEntity, Long>
        implements NotificacionRepository {

    public JpaNotificacionRepository(JpaContexto contexto) {
        super(contexto, NotificacionJpaEntity.class);
    }

    @Override
    public Notificacion guardar(Notificacion notificacion) {
        return escribir(em -> {
            NotificacionJpaEntity e;
            if (notificacion.getId() == null) {
                e = new NotificacionJpaEntity();
                NotificacionMapper.aplicar(notificacion, e);
                em.persist(e);
            } else {
                e = em.find(NotificacionJpaEntity.class, notificacion.getId());
                if (e == null) {
                    throw new EntityNotFoundException("No existe la notificacion " + notificacion.getId());
                }
                NotificacionMapper.aplicar(notificacion, e);
            }
            em.flush();
            NotificacionMapper.confirmar(notificacion, e);
            return notificacion;
        });
    }

    @Override
    public Optional<Notificacion> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return leer(em -> Optional.ofNullable(em.find(NotificacionJpaEntity.class, id)).map(NotificacionMapper::aDominio));
    }

    /** Bandeja del destinatario, la mas reciente primero. Usa el indice (destinatario_id, leida). */
    @Override
    public Pagina<Notificacion> buscarPorDestinatario(Long destinatarioId, boolean soloNoLeidas, PaginaRequest pagina) {
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("destinatarioId", destinatarioId);
            String desde = "from NotificacionJpaEntity n where n.destinatarioId = :destinatarioId";
            if (soloNoLeidas) {
                desde += " and n.leida = :leida";
                parametros.put("leida", false);
            }
            long total = contar(em, "select count(n) " + desde, parametros);
            if (total == 0 || pagina.offset() >= total) {
                return new Pagina<Notificacion>(List.of(), pagina.pagina(), pagina.tamano(), total);
            }
            List<NotificacionJpaEntity> filas = consulta(em, "select n " + desde + " order by n.fecha desc, n.id desc",
                    NotificacionJpaEntity.class, parametros)
                    .setFirstResult(pagina.offset())
                    .setMaxResults(pagina.tamano())
                    .getResultList();
            List<Notificacion> bandeja = new ArrayList<>(filas.size());
            for (NotificacionJpaEntity fila : filas) {
                bandeja.add(NotificacionMapper.aDominio(fila));
            }
            return new Pagina<>(bandeja, pagina.pagina(), pagina.tamano(), total);
        });
    }
}
