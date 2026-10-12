package com.uade.skillswap.support;

import com.uade.skillswap.domain.model.wallet.MovimientoCredito;
import com.uade.skillswap.domain.model.wallet.Wallet;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.WalletRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Fake de {@link WalletRepository}: persiste los movimientos nuevos con id y los limpia del agregado,
 * igual que el contrato del repositorio real.
 */
public class InMemoryWalletRepository implements WalletRepository {

    private final Map<Long, Wallet> wallets = new LinkedHashMap<>();
    private final Map<Long, List<MovimientoCredito>> movimientos = new HashMap<>();
    private long secuencia = 0;
    private long secuenciaMovimientos = 0;

    @Override
    public Wallet guardar(Wallet wallet) {
        if (wallet.getId() == null) {
            Reflejo.asignar(wallet, "id", ++secuencia);
        }
        Reflejo.asignar(wallet, "version", Reflejo.version(wallet.getVersion()));
        List<MovimientoCredito> historial = movimientos.computeIfAbsent(wallet.getPerfilId(), k -> new ArrayList<>());
        for (MovimientoCredito m : wallet.getMovimientosNuevos()) {
            historial.add(new MovimientoCredito(++secuenciaMovimientos, m.getPerfilId(), m.getTipo(), m.getMonto(),
                    m.getSaldoPosterior(), m.getContratacionId(), m.getFecha()));
        }
        wallet.limpiarMovimientosNuevos();
        wallets.put(wallet.getPerfilId(), wallet);
        return wallet;
    }

    @Override
    public Optional<Wallet> buscarPorPerfilId(Long perfilId) {
        return Optional.ofNullable(wallets.get(perfilId));
    }

    /** Movimientos del mas reciente al mas antiguo. */
    @Override
    public Pagina<MovimientoCredito> movimientos(Long perfilId, PaginaRequest pagina) {
        List<MovimientoCredito> todos = new ArrayList<>(movimientos.getOrDefault(perfilId, List.of()));
        Collections.reverse(todos);
        return Paginador.paginar(todos, pagina);
    }
}
