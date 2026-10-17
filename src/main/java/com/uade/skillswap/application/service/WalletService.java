package com.uade.skillswap.application.service;

import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.WalletDto;
import com.uade.skillswap.application.mapper.Mapeador;
import com.uade.skillswap.application.port.in.WalletUseCase;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.WalletRepository;

/** Consultas de la billetera propia. El movimiento de dinero lo orquestan los otros servicios. */
public class WalletService implements WalletUseCase {

    private final WalletRepository walletRepository;

    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }

    @Override
    public WalletDto consultar(Long actorId) {
        return Mapeador.wallet(Cargas.wallet(walletRepository, actorId));
    }

    @Override
    public Pagina<MovimientoDto> movimientos(Long actorId, PaginaRequest pagina) {
        Entradas.exigirPagina(pagina);
        Cargas.wallet(walletRepository, actorId); // RecursoNoEncontradoException si el perfil no tiene wallet
        return walletRepository.movimientos(actorId, pagina).mapear(Mapeador::movimiento);
    }
}
