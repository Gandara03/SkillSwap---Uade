package com.uade.skillswap.infrastructure.persistence.jpa.repository;

import com.uade.skillswap.domain.model.wallet.MovimientoCredito;
import com.uade.skillswap.domain.model.wallet.Wallet;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.WalletRepository;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.MovimientoCreditoJpaEntity;
import com.uade.skillswap.infrastructure.persistence.jpa.entity.WalletJpaEntity;
import com.uade.skillswap.infrastructure.persistence.jpa.mapper.WalletMapper;
import jakarta.persistence.EntityNotFoundException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JpaWalletRepository extends SimpleJpaRepository<WalletJpaEntity, Long> implements WalletRepository {

    public JpaWalletRepository(JpaContexto contexto) {
        super(contexto, WalletJpaEntity.class);
    }

    /**
     * Saldo y movimientos nuevos se confirman juntos (misma transaccion). El saldo lleva @Version: dos debitos
     * concurrentes sobre la misma wallet no pueden pisarse. Despues de insertar, los movimientos se limpian del agregado.
     */
    @Override
    public Wallet guardar(Wallet wallet) {
        return escribir(em -> {
            WalletJpaEntity e;
            if (wallet.getId() == null) {
                e = new WalletJpaEntity();
                WalletMapper.aplicar(wallet, e);
                em.persist(e);
            } else {
                e = em.find(WalletJpaEntity.class, wallet.getId());
                if (e == null) {
                    throw new EntityNotFoundException("No existe la wallet " + wallet.getId());
                }
                exigirVersion("La wallet", wallet.getId(), wallet.getVersion(), e.getVersion());
                WalletMapper.aplicar(wallet, e);
            }
            for (MovimientoCredito m : wallet.getMovimientosNuevos()) {
                em.persist(WalletMapper.movimientoAEntidad(m));
            }
            em.flush();
            WalletMapper.confirmar(wallet, e);
            wallet.limpiarMovimientosNuevos();
            return wallet;
        });
    }

    @Override
    public Optional<Wallet> buscarPorPerfilId(Long perfilId) {
        if (perfilId == null) {
            return Optional.empty();
        }
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("perfilId", perfilId);
            return listar(em, "select w from WalletJpaEntity w where w.perfilId = :perfilId", WalletJpaEntity.class,
                    parametros).stream().findFirst().map(WalletMapper::aDominio);
        });
    }

    /** Historial de movimientos, del mas reciente al mas antiguo. */
    @Override
    public Pagina<MovimientoCredito> movimientos(Long perfilId, PaginaRequest pagina) {
        return leer(em -> {
            Map<String, Object> parametros = new HashMap<>();
            parametros.put("perfilId", perfilId);
            String desde = "from MovimientoCreditoJpaEntity m where m.perfilId = :perfilId";
            long total = contar(em, "select count(m) " + desde, parametros);
            if (total == 0 || pagina.offset() >= total) {
                return new Pagina<MovimientoCredito>(List.of(), pagina.pagina(), pagina.tamano(), total);
            }
            List<MovimientoCreditoJpaEntity> filas = consulta(em, "select m " + desde + " order by m.id desc",
                    MovimientoCreditoJpaEntity.class, parametros)
                    .setFirstResult(pagina.offset())
                    .setMaxResults(pagina.tamano())
                    .getResultList();
            List<MovimientoCredito> movimientos = new ArrayList<>(filas.size());
            for (MovimientoCreditoJpaEntity fila : filas) {
                movimientos.add(WalletMapper.movimientoADominio(fila));
            }
            return new Pagina<>(movimientos, pagina.pagina(), pagina.tamano(), total);
        });
    }
}
