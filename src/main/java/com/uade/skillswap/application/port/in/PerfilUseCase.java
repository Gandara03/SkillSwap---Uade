package com.uade.skillswap.application.port.in;

import com.uade.skillswap.application.command.ActualizarPerfilCommand;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.dto.PerfilDto;

public interface PerfilUseCase {

    /** Crea el perfil (y su wallet con el bono inicial) para el usuario autenticado. Un perfil por userId. */
    PerfilDto registrar(RegistrarPerfilCommand comando);

    PerfilDto actualizar(Long actorId, ActualizarPerfilCommand comando);

    PerfilDto obtener(Long perfilId);

    /** Baja logica: conserva el historial de operaciones. */
    void darDeBaja(Long actorId);
}
