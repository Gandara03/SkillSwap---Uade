package com.uade.skillswap.application.mapper;

import com.uade.skillswap.application.command.BloqueDisponibilidadDto;
import com.uade.skillswap.application.dto.CalificacionDto;
import com.uade.skillswap.application.dto.ComprobanteDto;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.FranjaDto;
import com.uade.skillswap.application.dto.HistorialDto;
import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PropuestaReprogramacionDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.dto.WalletDto;
import com.uade.skillswap.domain.model.calificacion.Calificacion;
import com.uade.skillswap.domain.model.contratacion.Comprobante;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.contratacion.PropuestaReprogramacion;
import com.uade.skillswap.domain.model.historial.HistorialEstado;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.application.precio.PrecioPublicacion;
import com.uade.skillswap.domain.model.publicacion.Franja;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.vo.BloqueDisponibilidad;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.domain.model.wallet.MovimientoCredito;
import com.uade.skillswap.domain.model.wallet.Wallet;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Traduce entidades de dominio a DTOs de salida (los DTOs nunca exponen entidades ni colecciones mutables
 * del dominio). Metodos estaticos y sin estado.
 */
public final class Mapeador {

    private Mapeador() { }

    // ---------- perfil ----------

    public static PerfilDto perfil(Perfil p) {
        return new PerfilDto(p.getId(), p.getUserId(), p.getTipo(), p.getNombreVisible(), p.getBiografia(),
                p.getZonaHoraria() == null ? null : p.getZonaHoraria().getId(),
                nombresDeHabilidades(p.getHabilidades()), new LinkedHashSet<>(p.getIntereses()),
                bloques(p.getDisponibilidad()), p.getCanalPreferido(), p.getReputacion(),
                p.getCantidadCalificaciones(), p.estaActivo());
    }

    public static Set<String> nombresDeHabilidades(Set<Habilidad> habilidades) {
        Set<String> nombres = new LinkedHashSet<>();
        for (Habilidad h : habilidades) {
            nombres.add(h.getNombre());
        }
        return nombres;
    }

    public static List<BloqueDisponibilidadDto> bloques(List<BloqueDisponibilidad> bloques) {
        return bloques.stream().map(b -> new BloqueDisponibilidadDto(b.getDia(), b.getDesde(), b.getHasta())).toList();
    }

    // ---------- publicacion ----------

    /** @param precio precio con promociones; null si el borrador todavia no tiene precio base. */
    public static PublicacionDto publicacion(Publicacion p, PrecioPublicacion precio) {
        return new PublicacionDto(p.getId(), p.getTipo(), p.getAutorId(), p.getTitulo(), p.getDescripcion(),
                p.getCategoria(), p.getModalidad(), p.getDuracionMinutos(), p.getCupo(), p.getCupoDisponible(),
                monto(p.getPrecioBase()), precio == null ? null : monto(precio.calcular()),
                precio == null ? List.of() : List.copyOf(precio.detalle()), p.getPoliticaCancelacion(),
                new LinkedHashSet<>(p.getEtiquetas()), nombresDeHabilidades(p.getHabilidades()),
                p.getFranjas().stream().map(Mapeador::franja).toList(), p.getEstado(),
                historial(p.getHistorial()), p.getCreadaEn(), p.getPublicadaEn());
    }

    public static FranjaDto franja(Franja f) {
        return new FranjaDto(f.getId(), f.getInicio(), f.getFin(), f.estaOcupada());
    }

    /** @param autor puede ser null si el perfil ya no esta disponible (se informan datos vacios). */
    public static PublicacionResumenDto resumen(Publicacion p, Perfil autor, PrecioPublicacion precio) {
        return new PublicacionResumenDto(p.getId(), p.getTipo(), p.getTitulo(), p.getCategoria(), p.getModalidad(),
                monto(p.getPrecioBase()), precio == null ? null : monto(precio.calcular()), p.getAutorId(),
                autor == null ? null : autor.getNombreVisible(), autor == null ? 0d : autor.getReputacion(),
                p.getCupoDisponible(), p.getEstado());
    }

    // ---------- contratacion ----------

    public static ContratacionDto contratacion(Contratacion c, String publicacionTitulo) {
        return new ContratacionDto(c.getId(), c.getPublicacionId(), publicacionTitulo, c.getClienteId(),
                c.getProveedorId(), c.getFranjaId(), c.getMensaje(), c.getObjetivo(), monto(c.getPrecioFinal()),
                c.getEstado(), propuesta(c.getPropuestaReprogramacion()), historial(c.getHistorial()),
                c.getCreadaEn(), c.getPagadaEn(), c.getCompletadaEn());
    }

    public static PropuestaReprogramacionDto propuesta(PropuestaReprogramacion p) {
        return p == null ? null : new PropuestaReprogramacionDto(p.getNuevaFranjaId(), p.getPropuestaPorId(), p.getFecha());
    }

    public static List<HistorialDto> historial(List<HistorialEstado> historial) {
        return historial.stream()
                .map(h -> new HistorialDto(h.getEstadoAnterior(), h.getEstadoNuevo(), h.getActorId(), h.getMotivo(),
                        h.getFecha()))
                .toList();
    }

    public static ComprobanteDto comprobante(Comprobante c) {
        return new ComprobanteDto(c.getNumero(), c.getContratacionId(), c.getPublicacionId(), c.getClienteId(),
                c.getProveedorId(), monto(c.getMonto()), c.getPagadoEn(), c.getCompletadoEn());
    }

    // ---------- calificacion, notificacion, wallet ----------

    public static CalificacionDto calificacion(Calificacion c) {
        return new CalificacionDto(c.getId(), c.getContratacionId(), c.getAutorId(), c.getDestinatarioId(),
                c.getPuntaje().getValor(), c.getComentario(), c.getFecha());
    }

    public static NotificacionDto notificacion(Notificacion n) {
        return new NotificacionDto(n.getId(), n.getTipoEvento(), n.getMensaje(), n.getCanal(), n.estaLeida(),
                n.getFecha());
    }

    public static WalletDto wallet(Wallet w) {
        return new WalletDto(w.getPerfilId(), monto(w.getSaldo()));
    }

    public static MovimientoDto movimiento(MovimientoCredito m) {
        return new MovimientoDto(m.getId(), m.getTipo(), monto(m.getMonto()), monto(m.getSaldoPosterior()),
                m.getContratacionId(), m.getFecha());
    }

    public static BigDecimal monto(SkillCoins coins) {
        return coins == null ? null : coins.getMonto();
    }
}
