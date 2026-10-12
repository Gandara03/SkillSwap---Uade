package com.uade.skillswap.presentation.ui;

import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.HistorialDto;
import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Formatos de texto de los DTO para la consola (compartidos por el menu y la demo). */
public final class Formato {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    private Formato() {
    }

    public static String monto(BigDecimal monto) {
        return monto == null ? "-" : monto.toPlainString() + " SC";
    }

    public static String fecha(LocalDateTime fecha) {
        return fecha == null ? "-" : FECHA_HORA.format(fecha);
    }

    public static String publicacion(PublicacionResumenDto p) {
        String precio = p.precioFinal() != null && p.precioBase() != null && p.precioFinal().compareTo(p.precioBase()) != 0
                ? monto(p.precioBase()) + " -> " + monto(p.precioFinal())
                : monto(p.precioFinal());
        return "#%d [%s] %s | %s | %s (rep. %.1f) | cupo %d".formatted(p.id(), p.tipo(), p.titulo(), precio,
                p.autorNombre(), p.autorReputacion(), p.cupoDisponible());
    }

    public static String contratacion(ContratacionDto c) {
        return "#%d '%s' | %s | %s".formatted(c.id(), c.publicacionTitulo(), c.estado(), monto(c.precioFinal()));
    }

    public static String movimiento(MovimientoDto m) {
        return "%s %-12s %s | saldo %s".formatted(fecha(m.fecha()), m.tipo(), monto(m.monto()), monto(m.saldoResultante()));
    }

    public static String notificacion(NotificacionDto n) {
        return "%s [%s]%s %s: %s".formatted(fecha(n.fecha()), n.canal(), n.leida() ? "" : " (nueva)", n.tipoEvento(),
                n.mensaje());
    }

    public static String historial(HistorialDto h) {
        String anterior = h.estadoAnterior() == null ? "(inicio)" : h.estadoAnterior();
        return "%s %s -> %s | %s".formatted(fecha(h.fecha()), anterior, h.estadoNuevo(),
                h.motivo() == null ? "" : h.motivo());
    }
}
