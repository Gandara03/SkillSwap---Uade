package com.uade.skillswap.application.dto;

import com.uade.skillswap.application.command.BloqueDisponibilidadDto;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;

import java.util.List;
import java.util.Set;

public record PerfilDto(
        Long id,
        String userId,
        TipoPerfil tipo,
        String nombreVisible,
        String biografia,
        String zonaHoraria,
        Set<String> habilidades,
        Set<String> intereses,
        List<BloqueDisponibilidadDto> disponibilidad,
        CanalNotificacion canalPreferido,
        double reputacion,
        int cantidadCalificaciones,
        boolean activo) { }
