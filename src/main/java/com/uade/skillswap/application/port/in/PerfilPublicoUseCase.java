package com.uade.skillswap.application.port.in;

import com.uade.skillswap.application.dto.PerfilPublicoDto;

public interface PerfilPublicoUseCase {

    /**
     * Resumen publico con reputacion, ultimas calificaciones, publicaciones activas e historial resumido
     * (contrataciones completadas como proveedor).
     */
    PerfilPublicoDto verPerfilPublico(Long perfilId);
}
