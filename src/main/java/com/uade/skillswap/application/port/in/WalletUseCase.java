package com.uade.skillswap.application.port.in;

import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.WalletDto;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

public interface WalletUseCase {

    WalletDto consultar(Long actorId);

    Pagina<MovimientoDto> movimientos(Long actorId, PaginaRequest pagina);
}
