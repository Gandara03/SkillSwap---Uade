package com.uade.skillswap.application.notificacion;

import com.uade.skillswap.application.port.out.CanalNotificacionFactory;
import com.uade.skillswap.application.port.out.RelojPort;
import com.uade.skillswap.application.port.out.TransaccionPort;
import com.uade.skillswap.domain.model.contratacion.observer.ContratacionEvent;
import com.uade.skillswap.domain.model.contratacion.observer.ContratacionObserver;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.application.port.out.NotificacionRepository;
import com.uade.skillswap.application.port.out.PerfilRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Observer concreto de Contratacion. Ante cada evento notifica al destinatario por DOS tipos de notificacion:
 * siempre la INTERNA (bandeja) y, si su canal preferido es otro, tambien por ese canal (Adapter elegido con
 * la Simple Factory). Persiste en su propia transaccion; el envio externo ocurre despues de confirmarla.
 */
public class NotificadorObserver implements ContratacionObserver {

    private static final Logger LOG = Logger.getLogger(NotificadorObserver.class.getName());

    private final NotificacionRepository notificacionRepository;
    private final PerfilRepository perfilRepository;
    private final CanalNotificacionFactory canales;
    private final TransaccionPort transaccion;
    private final RelojPort reloj;

    public NotificadorObserver(NotificacionRepository notificacionRepository, PerfilRepository perfilRepository,
                               CanalNotificacionFactory canales, TransaccionPort transaccion, RelojPort reloj) {
        this.notificacionRepository = notificacionRepository;
        this.perfilRepository = perfilRepository;
        this.canales = canales;
        this.transaccion = transaccion;
        this.reloj = reloj;
    }

    @Override
    public void actualizar(ContratacionEvent evento) {
        Long destinatarioId = destinatarioDe(evento);
        Perfil destinatario = perfilRepository.buscarPorId(destinatarioId).orElse(null);
        if (destinatario == null) {
            LOG.warning("Se omite la notificacion " + evento.getTipo() + ": no existe el perfil " + destinatarioId);
            return;
        }
        String mensaje = mensajeDe(evento);
        LocalDateTime ahora = reloj.ahora();

        List<Notificacion> registradas = transaccion.ejecutar(() -> {
            List<Notificacion> lista = new ArrayList<>();
            for (CanalNotificacion canal : canalesPara(destinatario)) {
                lista.add(notificacionRepository.guardar(
                        Notificacion.crear(destinatarioId, evento.getTipo(), mensaje, canal, ahora)));
            }
            return lista;
        });

        // Un canal externo caido no debe impedir los otros ni deshacer lo ya registrado.
        for (Notificacion notificacion : registradas) {
            try {
                canales.crear(notificacion.getCanal()).enviar(notificacion, destinatario);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Fallo el envio por " + notificacion.getCanal() + " al perfil "
                        + destinatarioId, e);
            }
        }
    }

    /** Siempre INTERNA; ademas el canal preferido del perfil si es distinto. */
    private static Set<CanalNotificacion> canalesPara(Perfil destinatario) {
        Set<CanalNotificacion> canales = new LinkedHashSet<>();
        canales.add(CanalNotificacion.INTERNA);
        if (destinatario.getCanalPreferido() != null) {
            canales.add(destinatario.getCanalPreferido());
        }
        return canales;
    }

    private static Long destinatarioDe(ContratacionEvent e) {
        return switch (e.getTipo()) {
            case SOLICITUD_RECIBIDA, PAGO_CONFIRMADO -> e.getProveedorId();
            case SOLICITUD_ACEPTADA, SOLICITUD_RECHAZADA -> e.getClienteId();
            case REPROGRAMACION_PROPUESTA, REPROGRAMACION_ACEPTADA, REPROGRAMACION_RECHAZADA, CANCELACION,
                    SESION_COMPLETADA -> contraparteDelActor(e);
        };
    }

    private static Long contraparteDelActor(ContratacionEvent e) {
        return e.getClienteId().equals(e.getActorId()) ? e.getProveedorId() : e.getClienteId();
    }

    private static String mensajeDe(ContratacionEvent e) {
        String ref = "contratacion #" + e.getContratacionId();
        String detalle = e.getDetalle() == null || e.getDetalle().isBlank() ? "" : " (" + e.getDetalle() + ")";
        return switch (e.getTipo()) {
            case SOLICITUD_RECIBIDA -> "Recibiste una nueva solicitud, " + ref;
            case SOLICITUD_ACEPTADA -> "Tu solicitud fue aceptada, " + ref;
            case SOLICITUD_RECHAZADA -> "Tu solicitud fue rechazada, " + ref + detalle;
            case PAGO_CONFIRMADO -> "Se confirmo el pago de la " + ref + detalle;
            case REPROGRAMACION_PROPUESTA -> "Te proponen reprogramar la " + ref + detalle;
            case REPROGRAMACION_ACEPTADA -> "Aceptaron reprogramar la " + ref + detalle;
            case REPROGRAMACION_RECHAZADA -> "Rechazaron reprogramar la " + ref;
            case CANCELACION -> "Se cancelo la " + ref + detalle;
            case SESION_COMPLETADA -> "Se completo la sesion de la " + ref;
        };
    }
}
