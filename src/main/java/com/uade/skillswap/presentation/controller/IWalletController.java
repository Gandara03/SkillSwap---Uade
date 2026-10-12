package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.WalletDto;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

/** Contrato de la UI para la billetera de SkillCoins del usuario. */
public interface IWalletController {

    Resultado<WalletDto> consultar();

    Resultado<Pagina<MovimientoDto>> movimientos(PaginaRequest pagina);
}
