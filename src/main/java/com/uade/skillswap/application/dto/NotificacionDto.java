package com.uade.skillswap.application.dto;

import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;

import java.time.LocalDateTime;

public record NotificacionDto(Long id, TipoEventoContratacion tipoEvento, String mensaje, CanalNotificacion canal,
                              boolean leida, LocalDateTime fecha) { }
