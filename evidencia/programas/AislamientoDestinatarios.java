package com.uade.skillswap.application;

import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.notificacion.NotificadorObserver;
import com.uade.skillswap.application.port.out.NotificacionRepository;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.contratacion.observer.ContratacionEvent;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.infrastructure.notificacion.CanalNotificacionFactoryImpl;
import com.uade.skillswap.support.AplicacionEnMemoria;

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Re-revision H1: si falla el guardado de la notificacion del primer destinatario, ¿el segundo recibe la suya? */
public class AislamientoDestinatarios {
    public static void main(String[] args) {
        Logger.getLogger(NotificadorObserver.class.getName()).setLevel(Level.OFF);
        Escenario e = new Escenario();
        AplicacionEnMemoria app = e.app;
        PerfilDto ana = e.profesional("u-ana", "Ana", CanalNotificacion.EMAIL);
        PerfilDto bruno = e.profesional("u-bruno", "Bruno", CanalNotificacion.PUSH);
        NotificacionRepository fallaParaElCliente = new NotificacionRepository() {
            public Notificacion guardar(Notificacion n) {
                if (n.getDestinatarioId().equals(bruno.id())) {
                    throw new IllegalStateException("no se pudo guardar la notificacion del cliente");
                }
                return app.notificaciones.guardar(n);
            }
            public Optional<Notificacion> buscarPorId(Long id) { return app.notificaciones.buscarPorId(id); }
            public Pagina<Notificacion> buscarPorDestinatario(Long d, boolean s, PaginaRequest p) {
                return app.notificaciones.buscarPorDestinatario(d, s, p);
            }
        };
        NotificadorObserver observer = new NotificadorObserver(fallaParaElCliente, app.perfiles,
                new CanalNotificacionFactoryImpl(app.emailSdk, app.pushSdk, app.identidad), app.transaccion, app.reloj);
        int antes = app.notificaciones.todas().size();
        String propagada = "ninguna";
        try {
            observer.actualizar(new ContratacionEvent(TipoEventoContratacion.CANCELACION, 77L, 5L, bruno.id(), ana.id(),
                    bruno.id(), "Imprevisto", app.reloj.ahora()));
        } catch (RuntimeException ex) {
            propagada = ex.getClass().getSimpleName();
        }
        long deAna = app.notificaciones.todas().stream().skip(antes)
                .filter(n -> n.getDestinatarioId().equals(ana.id())).count();
        System.out.println("[H1] excepcion que sale de actualizar(): " + propagada);
        System.out.println("[H1] notificaciones de la proveedora (2do destinatario): " + deAna
                + "; correos enviados: " + app.emailSdk.enviados().size());
    }
}
