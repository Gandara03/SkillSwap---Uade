package com.uade.skillswap.application.port.out;

import com.uade.skillswap.domain.model.wallet.MovimientoCredito;
import com.uade.skillswap.domain.model.wallet.Wallet;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

import java.util.Optional;

public interface WalletRepository {
    /** Persiste el saldo y los movimientos nuevos del wallet y los limpia del agregado. */
    Wallet guardar(Wallet wallet);
    Optional<Wallet> buscarPorPerfilId(Long perfilId);
    Pagina<MovimientoCredito> movimientos(Long perfilId, PaginaRequest pagina);
}
