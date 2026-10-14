package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.ActualizarPerfilCommand;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PerfilPublicoDto;
import com.uade.skillswap.application.port.in.PerfilPublicoUseCase;
import com.uade.skillswap.application.port.in.PerfilUseCase;
import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.presentation.Resultado;

/** Adaptador delgado: traduce la UI a PerfilUseCase y PerfilPublicoUseCase. Sin reglas de negocio. */
public class PerfilController implements IPerfilController {

    private final PerfilUseCase perfiles;
    private final PerfilPublicoUseCase perfilPublico;
    private final SesionUseCase sesion;

    public PerfilController(PerfilUseCase perfiles, PerfilPublicoUseCase perfilPublico, SesionUseCase sesion) {
        this.perfiles = perfiles;
        this.perfilPublico = perfilPublico;
        this.sesion = sesion;
    }

    @Override
    public Resultado<PerfilDto> registrar(RegistrarPerfilCommand comando) {
        return Ejecucion.sinActor(() -> perfiles.registrar(comando));
    }

    @Override
    public Resultado<PerfilDto> actualizar(ActualizarPerfilCommand comando) {
        return Ejecucion.conActor(sesion, actor -> perfiles.actualizar(actor, comando));
    }

    @Override
    public Resultado<PerfilDto> miPerfil() {
        return Ejecucion.conActor(sesion, perfiles::obtener);
    }

    @Override
    public Resultado<PerfilPublicoDto> verPerfilPublico(Long perfilId) {
        return Ejecucion.sinActor(() -> perfilPublico.verPerfilPublico(perfilId));
    }

    @Override
    public Resultado<Void> darDeBaja() {
        return Ejecucion.conActorSinValor(sesion, perfiles::darDeBaja);
    }
}
