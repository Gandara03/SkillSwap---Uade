package com.uade.skillswap.application.port.in;

import com.uade.skillswap.application.dto.PerfilPublicoDto;

public interface PerfilPublicoUseCase {

    /** Resumen publico con reputacion, ultimas calificaciones y publicaciones activas. */
    PerfilPublicoDto verPerfilPublico(Long perfilId);
}
