package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.ActualizarPerfilCommand;
import com.uade.skillswap.application.command.BloqueDisponibilidadDto;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PerfilPublicoDto;
import com.uade.skillswap.application.port.in.PerfilPublicoUseCase;
import com.uade.skillswap.application.port.in.PerfilUseCase;
import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.presentation.Resultado;

import java.util.List;
import java.util.Set;

/** Adaptador delgado: valida la entrada y la traduce a PerfilUseCase y PerfilPublicoUseCase. Sin reglas de negocio. */
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
        return Ejecucion.sinActor(() -> {
            validar(comando);
            return perfiles.registrar(comando);
        });
    }

    @Override
    public Resultado<PerfilDto> actualizar(ActualizarPerfilCommand comando) {
        return Ejecucion.conActor(sesion, actor -> {
            validar(comando);
            return perfiles.actualizar(actor, comando);
        });
    }

    @Override
    public Resultado<PerfilDto> miPerfil() {
        return Ejecucion.conActor(sesion, perfiles::obtener);
    }

    @Override
    public Resultado<PerfilPublicoDto> verPerfilPublico(Long perfilId) {
        return Ejecucion.sinActor(() -> {
            ValidadorEntrada.exigirId(perfilId, "el perfil");
            return perfilPublico.verPerfilPublico(perfilId);
        });
    }

    @Override
    public Resultado<Void> darDeBaja() {
        return Ejecucion.conActorSinValor(sesion, perfiles::darDeBaja);
    }

    // ---------- validacion de entrada ----------

    private static void validar(RegistrarPerfilCommand c) {
        ValidadorEntrada.exigir(c, "los datos del perfil");
        ValidadorEntrada.exigirTexto(c.userId(), ValidadorEntrada.LARGO_USER_ID, "el userId");
        ValidadorEntrada.exigir(c.tipo(), "el tipo de perfil");
        validarDatos(c.nombreVisible(), c.biografia(), c.zonaHoraria(), c.habilidades(), c.intereses(),
                c.disponibilidad());
    }

    private static void validar(ActualizarPerfilCommand c) {
        ValidadorEntrada.exigir(c, "los datos del perfil");
        validarDatos(c.nombreVisible(), c.biografia(), c.zonaHoraria(), c.habilidades(), c.intereses(),
                c.disponibilidad());
    }

    /** Datos comunes a registrar y actualizar. La zona vacia significa "la de siempre" (lo resuelve el caso de uso). */
    private static void validarDatos(String nombreVisible, String biografia, String zonaHoraria,
                                     Set<String> habilidades, Set<String> intereses,
                                     List<BloqueDisponibilidadDto> disponibilidad) {
        ValidadorEntrada.exigirTexto(nombreVisible, ValidadorEntrada.LARGO_NOMBRE, "el nombre visible");
        ValidadorEntrada.exigirLargo(biografia, ValidadorEntrada.LARGO_BIOGRAFIA, "la biografia");
        ValidadorEntrada.exigirZonaHoraria(zonaHoraria);
        ValidadorEntrada.exigirTextos(habilidades, ValidadorEntrada.LARGO_ITEM, "las habilidades");
        ValidadorEntrada.exigirTextos(intereses, ValidadorEntrada.LARGO_ITEM, "los intereses");
        if (disponibilidad != null) {
            for (BloqueDisponibilidadDto bloque : disponibilidad) {
                ValidadorEntrada.exigir(bloque, "el bloque de disponibilidad");
                ValidadorEntrada.exigir(bloque.dia(), "el dia del bloque de disponibilidad");
                ValidadorEntrada.exigir(bloque.desde(), "el inicio del bloque de disponibilidad");
                ValidadorEntrada.exigir(bloque.hasta(), "el fin del bloque de disponibilidad");
                ValidadorEntrada.exigirAnterior(bloque.desde(), bloque.hasta(), "el bloque de disponibilidad");
            }
        }
    }
}
