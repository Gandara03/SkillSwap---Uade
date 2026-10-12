package com.uade.skillswap.application.command;

import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;

import java.util.List;
import java.util.Set;

public record ActualizarPerfilCommand(
        String nombreVisible,
        String biografia,
        String zonaHoraria,
        Set<String> habilidades,
        Set<String> intereses,
        List<BloqueDisponibilidadDto> disponibilidad,
        CanalNotificacion canalPreferido) { }
