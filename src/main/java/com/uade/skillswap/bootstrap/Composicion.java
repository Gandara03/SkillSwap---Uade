package com.uade.skillswap.bootstrap;

import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.facade.PerfilPublicoFacade;
import com.uade.skillswap.application.notificacion.NotificadorObserver;
import com.uade.skillswap.application.port.out.CanalNotificacionFactory;
import com.uade.skillswap.application.port.out.RelojPort;
import com.uade.skillswap.application.port.out.TransaccionPort;
import com.uade.skillswap.application.service.BusquedaService;
import com.uade.skillswap.application.service.CalificacionService;
import com.uade.skillswap.application.service.ContratacionService;
import com.uade.skillswap.application.service.NotificacionService;
import com.uade.skillswap.application.service.PerfilService;
import com.uade.skillswap.application.service.PublicacionService;
import com.uade.skillswap.application.service.RecomendacionService;
import com.uade.skillswap.application.service.SesionService;
import com.uade.skillswap.application.service.WalletService;
import com.uade.skillswap.domain.model.contratacion.observer.ContratacionObserver;
import com.uade.skillswap.domain.model.politica.PoliticaCancelacionFactory;
import com.uade.skillswap.infrastructure.identidad.IdentidadMockAdapter;
import com.uade.skillswap.infrastructure.notificacion.CanalNotificacionFactoryImpl;
import com.uade.skillswap.infrastructure.notificacion.sdk.EmailSdkMock;
import com.uade.skillswap.infrastructure.notificacion.sdk.PushSdkMock;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.infrastructure.persistence.JpaTransaccionAdapter;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaCalificacionRepository;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaContratacionRepository;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaNotificacionRepository;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaPerfilRepository;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaPromocionRepository;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaPublicacionRepository;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaWalletRepository;
import com.uade.skillswap.infrastructure.time.RelojSistema;
import com.uade.skillswap.presentation.adaptador.UiControllersAdapter;
import com.uade.skillswap.presentation.controller.BusquedaController;
import com.uade.skillswap.presentation.controller.CalificacionController;
import com.uade.skillswap.presentation.controller.ContratacionController;
import com.uade.skillswap.presentation.controller.NotificacionController;
import com.uade.skillswap.presentation.controller.PerfilController;
import com.uade.skillswap.presentation.controller.PublicacionController;
import com.uade.skillswap.presentation.controller.RecomendacionController;
import com.uade.skillswap.presentation.controller.WalletController;
import com.uade.skillswap.presentation.ui.Autenticador;
import com.uade.skillswap.presentation.ui.swing.contrato.UiControllers;

import java.util.List;

/**
 * Composition root: el unico lugar que conoce las clases concretas de todas las capas y las cablea a mano
 * (inyeccion por constructor, sin framework). Las capas internas solo ven interfaces.
 */
public final class Composicion {

    private final JpaContexto contexto;
    private final EmailSdkMock emailSdk;
    private final PushSdkMock pushSdk;
    private final PerfilController perfilController;
    private final PublicacionController publicacionController;
    private final BusquedaController busquedaController;
    private final RecomendacionController recomendacionController;
    private final ContratacionController contratacionController;
    private final CalificacionController calificacionController;
    private final NotificacionController notificacionController;
    private final WalletController walletController;

    private Composicion(JpaContexto contexto, EmailSdkMock emailSdk, PushSdkMock pushSdk,
                        PerfilController perfil, PublicacionController publicacion,
                        BusquedaController busqueda, RecomendacionController recomendacion,
                        ContratacionController contratacion, CalificacionController calificacion,
                        NotificacionController notificacion, WalletController wallet) {
        this.contexto = contexto;
        this.emailSdk = emailSdk;
        this.pushSdk = pushSdk;
        this.perfilController = perfil;
        this.publicacionController = publicacion;
        this.busquedaController = busqueda;
        this.recomendacionController = recomendacion;
        this.contratacionController = contratacion;
        this.calificacionController = calificacion;
        this.notificacionController = notificacion;
        this.walletController = wallet;
    }

    public static Composicion crear(JpaContexto ctx, IdentidadMockAdapter identidad, SkillSwapConfig config) {
        // Persistencia (adapters de los puertos de repositorio del dominio)
        var perfiles = new JpaPerfilRepository(ctx);
        var publicaciones = new JpaPublicacionRepository(ctx);
        var contrataciones = new JpaContratacionRepository(ctx);
        var wallets = new JpaWalletRepository(ctx);
        var calificaciones = new JpaCalificacionRepository(ctx);
        var notificaciones = new JpaNotificacionRepository(ctx);
        var promociones = new JpaPromocionRepository(ctx);
        TransaccionPort transaccion = new JpaTransaccionAdapter(ctx);
        RelojPort reloj = new RelojSistema();

        // Canales de notificacion: un SDK por proveedor externo; la Simple Factory crea el adapter en cada pedido
        var emailSdk = new EmailSdkMock();
        var pushSdk = new PushSdkMock();
        CanalNotificacionFactory canales = new CanalNotificacionFactoryImpl(emailSdk, pushSdk, identidad);

        // Observers de las contrataciones: la lista se arma una sola vez.
        List<ContratacionObserver> observadores =
                List.of(new NotificadorObserver(notificaciones, perfiles, canales, transaccion, reloj));

        // Servicios de aplicacion (casos de uso)
        var sesion = new SesionService(identidad, perfiles);
        var perfilService = new PerfilService(perfiles, wallets, publicaciones, identidad, transaccion, reloj, config);
        var publicacionService = new PublicacionService(publicaciones, perfiles, promociones, transaccion, reloj);
        var contratacionService = new ContratacionService(contrataciones, publicaciones, perfiles, wallets,
                promociones, observadores, transaccion, reloj,
                new PoliticaCancelacionFactory(config.cancelacionFlexible(), config.cancelacionEstricta()));
        var busquedaService = new BusquedaService(publicaciones, perfiles, promociones, reloj);
        var recomendacionService = new RecomendacionService(publicaciones, perfiles, reloj, promociones, config);
        var calificacionService = new CalificacionService(calificaciones, contrataciones, perfiles, transaccion, reloj);
        var notificacionService = new NotificacionService(notificaciones);
        var walletService = new WalletService(wallets);
        // Facade: la vista publica del perfil coordina tres casos de uso
        var perfilPublico = new PerfilPublicoFacade(perfilService, calificacionService, publicacionService, config);

        // Controllers (presentacion): dependen solo de los puertos de entrada
        return new Composicion(ctx, emailSdk, pushSdk,
                new PerfilController(perfilService, perfilPublico, sesion),
                new PublicacionController(publicacionService, sesion),
                new BusquedaController(busquedaService),
                new RecomendacionController(recomendacionService, sesion),
                new ContratacionController(contratacionService, sesion),
                new CalificacionController(calificacionService, sesion),
                new NotificacionController(notificacionService, sesion),
                new WalletController(walletService, sesion));
    }

    public PerfilController getPerfilController() {
        return perfilController;
    }

    public PublicacionController getPublicacionController() {
        return publicacionController;
    }

    public BusquedaController getBusquedaController() {
        return busquedaController;
    }

    public RecomendacionController getRecomendacionController() {
        return recomendacionController;
    }

    public ContratacionController getContratacionController() {
        return contratacionController;
    }

    public CalificacionController getCalificacionController() {
        return calificacionController;
    }

    public NotificacionController getNotificacionController() {
        return notificacionController;
    }

    public WalletController getWalletController() {
        return walletController;
    }

    /** Correos enviados por el SDK simulado (los muestra la demo). */
    public EmailSdkMock getEmailSdk() {
        return emailSdk;
    }

    public PushSdkMock getPushSdk() {
        return pushSdk;
    }

    /** Contrato de la pantalla Swing sobre los controllers (Adapter); el login va a la identidad mock. */
    public UiControllers crearControllersUi(IdentidadMockAdapter identidad) {
        return new UiControllersAdapter(autenticador(identidad), perfilController, publicacionController,
                busquedaController, recomendacionController, contratacionController, calificacionController,
                notificacionController, walletController);
    }

    private static Autenticador autenticador(IdentidadMockAdapter identidad) {
        return new Autenticador() {
            @Override
            public void iniciarSesion(String userId) {
                identidad.iniciarSesion(userId);
            }

            @Override
            public void cerrarSesion() {
                identidad.cerrarSesion();
            }
        };
    }

    /** Libera los recursos de persistencia. */
    public void cerrar() {
        contexto.cerrar();
    }
}
