package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.WalletDto;
import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.application.port.in.WalletUseCase;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Adaptador delgado: traduce la UI a WalletUseCase. */
public class WalletController implements IWalletController {

    private final WalletUseCase wallet;
    private final SesionUseCase sesion;

    public WalletController(WalletUseCase wallet, SesionUseCase sesion) {
        this.wallet = wallet;
        this.sesion = sesion;
    }

    @Override
    public Resultado<WalletDto> consultar() {
        return Ejecucion.conActor(sesion, wallet::consultar);
    }

    @Override
    public Resultado<Pagina<MovimientoDto>> movimientos(PaginaRequest pagina) {
        return Ejecucion.conActor(sesion, actor -> wallet.movimientos(actor, pagina));
    }
}
