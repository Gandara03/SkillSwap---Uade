package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.ActualizarPerfilCommand;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PerfilPublicoDto;
import com.uade.skillswap.presentation.Resultado;

/** Contrato de la UI para perfiles. El actor siempre es el usuario con sesion iniciada. */
public interface IPerfilController {

    /** Crea el perfil (y su wallet con bono inicial) del usuario autenticado. */
    Resultado<PerfilDto> registrar(RegistrarPerfilCommand comando);

    Resultado<PerfilDto> actualizar(ActualizarPerfilCommand comando);

    Resultado<PerfilDto> miPerfil();

    Resultado<PerfilPublicoDto> verPerfilPublico(Long perfilId);

    /** Baja logica del perfil propio. */
    Resultado<Void> darDeBaja();
}
