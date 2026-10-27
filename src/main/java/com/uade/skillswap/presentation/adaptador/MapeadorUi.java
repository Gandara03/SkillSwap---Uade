package com.uade.skillswap.presentation.adaptador;

import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.FranjaCommand;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.FranjaDto;
import com.uade.skillswap.application.dto.MovimientoDto;
import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PerfilPublicoDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.model.wallet.TipoMovimiento;
import com.uade.skillswap.domain.query.CriterioOrden;
import com.uade.skillswap.presentation.ui.swing.contrato.Models;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Conversiones entre los DTO y commands de la aplicacion y los modelos de la pantalla. Sin estado y sin reglas:
 * solo cambia la forma de los datos (ids como texto, enums equivalentes, textos para mostrar).
 */
final class MapeadorUi {

    private MapeadorUi() {
    }

    // ---------- enums ----------

    /** Los enums de la pantalla y los de la aplicacion usan los mismos nombres de constantes. */
    static <E extends Enum<E>> E convertir(Enum<?> valor, Class<E> destino) {
        return valor == null ? null : Enum.valueOf(destino, valor.name());
    }

    static <E extends Enum<E>> List<E> convertir(List<? extends Enum<?>> valores, Class<E> destino) {
        return valores.stream().map(v -> convertir(v, destino)).toList();
    }

    /** Los criterios de orden tienen nombres distintos de un lado y del otro. */
    static CriterioOrden criterio(Models.OrdenBusqueda orden) {
        if (orden == null) {
            return CriterioOrden.RELEVANCIA;
        }
        return switch (orden) {
            case RELEVANCIA -> CriterioOrden.RELEVANCIA;
            case MEJOR_REPUTACION -> CriterioOrden.REPUTACION;
            case MENOR_PRECIO -> CriterioOrden.PRECIO_ASC;
            case MAS_RECIENTES -> CriterioOrden.RECIENTES;
        };
    }

    // ---------- publicaciones ----------

    static Models.Publicacion publicacion(PublicacionResumenDto p) {
        return new Models.Publicacion(String.valueOf(p.id()), p.titulo(), p.descripcion(), p.categoria(),
                convertir(p.modalidad(), Models.Modalidad.class), p.duracionMinutos(), p.precioFinal(),
                p.autorReputacion(), p.autorNombre(), List.copyOf(p.etiquetas()),
                Models.EstadoPublicacion.valueOf(p.estado()), conPromocion(p.precioBase(), p.precioFinal()),
                p.cupoDisponible(), convertir(p.accionesDelAutor(), Models.AccionPublicacion.class));
    }

    /** Publicacion recien creada o modificada: su autor es el usuario activo. */
    static Models.Publicacion publicacion(PublicacionDto p, PerfilDto autor) {
        return new Models.Publicacion(String.valueOf(p.id()), p.titulo(), p.descripcion(), p.categoria(),
                convertir(p.modalidad(), Models.Modalidad.class), p.duracionMinutos(), p.precioFinal(),
                autor.reputacion(), autor.nombreVisible(), List.copyOf(p.etiquetas()),
                Models.EstadoPublicacion.valueOf(p.estado()), conPromocion(p.precioBase(), p.precioFinal()),
                p.cupoDisponible(), convertir(p.accionesDelAutor(), Models.AccionPublicacion.class));
    }

    /** "Destacada" en la pantalla = tiene una promocion vigente que baja el precio. */
    private static boolean conPromocion(BigDecimal precioBase, BigDecimal precioFinal) {
        return precioBase != null && precioFinal != null && precioFinal.compareTo(precioBase) < 0;
    }

    /** El formulario trae una sola franja (inicio y duracion); null si no se cargaron datos. */
    static CrearPublicacionCommand comando(Models.NuevaPublicacion d) {
        if (d == null) {
            return null;
        }
        List<FranjaCommand> franjas = d.inicioFranja() == null ? List.of()
                : List.of(new FranjaCommand(d.inicioFranja(), d.inicioFranja().plusMinutes(d.minutosFranja())));
        return new CrearPublicacionCommand(convertir(d.tipo(), TipoPublicacion.class), d.titulo(), d.descripcion(),
                d.categoria(), convertir(d.modalidad(), Modalidad.class), d.duracionMinutos(), d.cupo(), d.precio(),
                conjunto(d.etiquetas()), conjunto(d.habilidades()),
                convertir(d.politica(), TipoPoliticaCancelacion.class), franjas);
    }

    private static Set<String> conjunto(List<String> valores) {
        return valores == null ? null : new LinkedHashSet<>(valores);
    }

    static Models.Franja franja(FranjaDto f) {
        return new Models.Franja(String.valueOf(f.id()), f.inicio(), f.fin());
    }

    // ---------- contrataciones ----------

    /**
     * @param contraparte la otra parte, vista desde el usuario activo. La fecha es el inicio de la franja reservada
     *                    (o la de la solicitud, si la publicacion ya no esta disponible).
     */
    static Models.Contratacion contratacion(ContratacionDto c, String contraparte) {
        LocalDateTime fecha = c.franjaInicio() != null ? c.franjaInicio() : c.creadaEn();
        return new Models.Contratacion(String.valueOf(c.id()), c.publicacionTitulo(), contraparte, fecha,
                c.precioFinal(), Models.EstadoContratacion.valueOf(c.estado()),
                convertir(c.acciones(), Models.AccionContratacion.class));
    }

    // ---------- wallet, notificaciones y perfil ----------

    /** En la aplicacion el monto es siempre positivo y el tipo dice si suma o resta; la pantalla lo quiere con signo. */
    static Models.Movimiento movimiento(MovimientoDto m) {
        String concepto = concepto(m.tipo())
                + (m.contratacionId() == null ? "" : " · contratación #" + m.contratacionId());
        BigDecimal importe = m.tipo().esCredito() ? m.monto() : m.monto().negate();
        return new Models.Movimiento(concepto, m.fecha(), importe, m.saldoResultante());
    }

    private static String concepto(TipoMovimiento tipo) {
        return switch (tipo) {
            case BONO_INICIAL -> "Bono de bienvenida";
            case PAGO -> "Pago";
            case COBRO -> "Cobro";
            case REINTEGRO -> "Reintegro";
        };
    }

    static Models.Notificacion notificacion(NotificacionDto n) {
        return new Models.Notificacion(String.valueOf(n.id()), titulo(n.tipoEvento()), n.mensaje(), n.fecha(),
                n.leida(), n.canal() == null ? null : n.canal().name());
    }

    private static String titulo(TipoEventoContratacion tipo) {
        return switch (tipo) {
            case SOLICITUD_RECIBIDA -> "Nueva solicitud";
            case SOLICITUD_ACEPTADA -> "Solicitud aceptada";
            case SOLICITUD_RECHAZADA -> "Solicitud rechazada";
            case PAGO_CONFIRMADO -> "Pago confirmado";
            case REPROGRAMACION_PROPUESTA -> "Reprogramación propuesta";
            case REPROGRAMACION_ACEPTADA -> "Reprogramación aceptada";
            case REPROGRAMACION_RECHAZADA -> "Reprogramación rechazada";
            case CANCELACION -> "Contratación cancelada";
            case SESION_COMPLETADA -> "Sesión completada";
        };
    }

    /** Datos propios del perfil y lo que arma la vista publica (reputacion y ultimas calificaciones). */
    static Models.Perfil perfil(PerfilDto yo, PerfilPublicoDto publico, int proyectosCompletados) {
        List<Models.Resena> resenas = publico.ultimasCalificaciones().stream()
                .map(c -> new Models.Resena(c.puntaje(), c.comentario(), c.fecha()))
                .toList();
        return new Models.Perfil(yo.userId(), yo.nombreVisible(), rol(yo.tipo()), yo.biografia(),
                List.copyOf(yo.habilidades()), publico.reputacion(), publico.cantidadCalificaciones(),
                proyectosCompletados, resenas);
    }

    private static String rol(TipoPerfil tipo) {
        return tipo == TipoPerfil.ORGANIZACION ? "Organización" : "Profesional";
    }
}
