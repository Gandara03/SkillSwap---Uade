package com.uade.skillswap.application.command;

import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;

import java.util.List;
import java.util.Set;

/** El userId viene del servicio de identidad (se valida contra IdentidadPort), no del cliente. */
public record RegistrarPerfilCommand(
        String userId,
        String nombreVisible,
        TipoPerfil tipo,
        String zonaHoraria,
        String biografia,
        Set<String> habilidades,
        Set<String> intereses,
        List<BloqueDisponibilidadDto> disponibilidad,
        CanalNotificacion canalPreferido) { }
