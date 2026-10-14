package com.uade.skillswap.bootstrap;

import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.facade.PerfilPublicoFacade;
import com.uade.skillswap.application.notificacion.NotificadorObserver;
import com.uade.skillswap.application.port.in.SesionUseCase;
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
import com.uade.skillswap.presentation.controller.BusquedaController;
import com.uade.skillswap.presentation.controller.CalificacionController;
import com.uade.skillswap.presentation.controller.ContratacionController;
import com.uade.skillswap.presentation.controller.IBusquedaController;
import com.uade.skillswap.presentation.controller.ICalificacionController;
import com.uade.skillswap.presentation.controller.IContratacionController;
import com.uade.skillswap.presentation.controller.INotificacionController;
import com.uade.skillswap.presentation.controller.IPerfilController;
import com.uade.skillswap.presentation.controller.IPublicacionController;
import com.uade.skillswap.presentation.controller.IRecomendacionController;
import com.uade.skillswap.presentation.controller.IWalletController;
import com.uade.skillswap.presentation.controller.NotificacionController;
import com.uade.skillswap.presentation.controller.PerfilController;
import com.uade.skillswap.presentation.controller.PublicacionController;
import com.uade.skillswap.presentation.controller.RecomendacionController;
import com.uade.skillswap.presentation.controller.WalletController;
import com.uade.skillswap.presentation.ui.Autenticador;
import com.uade.skillswap.presentation.ui.MenuConsola;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

/**
 * Composition root: el unico lugar que conoce las clases concretas de todas las capas y las cablea a mano
 * (inyeccion por constructor, sin framework). Las capas internas solo ven interfaces.
 */
public final class Composicion {

    private final JpaContexto contexto;
    private final SkillSwapConfig config;
    private final EmailSdkMock emailSdk;
    private final PushSdkMock pushSdk;
    private final SesionUseCase sesion;
    private final IPerfilController perfilController;
    private final IPublicacionController publicacionController;
    private final IBusquedaController busquedaController;
    private final IRecomendacionController recomendacionController;
    private final IContratacionController contratacionController;
    private final ICalificacionController calificacionController;
    private final INotificacionController notificacionController;
    private final IWalletController walletController;

    private Composicion(JpaContexto contexto, SkillSwapConfig config, EmailSdkMock emailSdk, PushSdkMock pushSdk,
                        SesionUseCase sesion, IPerfilController perfil, IPublicacionController publicacion,
                        IBusquedaController busqueda, IRecomendacionController recomendacion,
                        IContratacionController contratacion, ICalificacionController calificacion,
                        INotificacionController notificacion, IWalletController wallet) {
        this.contexto = contexto;
        this.config = config;
        this.emailSdk = emailSdk;
        this.pushSdk = pushSdk;
        this.sesion = sesion;
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

        // Observer de contrataciones: la lista de interesados se arma una sola vez, aca. Hoy hay uno solo, que
        // persiste y envia las notificaciones despues del commit; un interesado nuevo se agrega a esta lista.
        List<ContratacionObserver> observadores =
                List.of(new NotificadorObserver(notificaciones, perfiles, canales, transaccion, reloj));

        // Servicios de aplicacion (casos de uso)
        var sesion = new SesionService(identidad, perfiles);
        var perfilService = new PerfilService(perfiles, wallets, publicaciones, identidad, transaccion, reloj, config);
        var publicacionService = new PublicacionService(publicaciones, perfiles, promociones, transaccion, reloj);
        var contratacionService = new ContratacionService(contrataciones, publicaciones, perfiles, wallets,
                promociones, observadores, transaccion, reloj);
        var busquedaService = new BusquedaService(publicaciones, perfiles, promociones, reloj);
        var recomendacionService = new RecomendacionService(publicaciones, perfiles, reloj, promociones, config);
        var calificacionService = new CalificacionService(calificaciones, contrataciones, perfiles, transaccion, reloj);
        var notificacionService = new NotificacionService(notificaciones);
        var walletService = new WalletService(wallets);
        // Facade: la vista publica del perfil coordina tres casos de uso
        var perfilPublico = new PerfilPublicoFacade(perfilService, calificacionService, publicacionService, config);

        // Controllers (presentacion): dependen solo de los puertos de entrada
        return new Composicion(ctx, config, emailSdk, pushSdk, sesion,
                new PerfilController(perfilService, perfilPublico, sesion),
                new PublicacionController(publicacionService, sesion),
                new BusquedaController(busquedaService),
                new RecomendacionController(recomendacionService, sesion),
                new ContratacionController(contratacionService, sesion),
                new CalificacionController(calificacionService, sesion),
                new NotificacionController(notificacionService, sesion),
                new WalletController(walletService, sesion));
    }

    public IPerfilController getPerfilController() {
        return perfilController;
    }

    public IPublicacionController getPublicacionController() {
        return publicacionController;
    }

    public IBusquedaController getBusquedaController() {
        return busquedaController;
    }

    public IRecomendacionController getRecomendacionController() {
        return recomendacionController;
    }

    public IContratacionController getContratacionController() {
        return contratacionController;
    }

    public ICalificacionController getCalificacionController() {
        return calificacionController;
    }

    public INotificacionController getNotificacionController() {
        return notificacionController;
    }

    public IWalletController getWalletController() {
        return walletController;
    }

    public SesionUseCase getSesion() {
        return sesion;
    }

    public SkillSwapConfig getConfig() {
        return config;
    }

    /** Correos "enviados" por el SDK simulado (la demo los muestra para evidenciar el segundo canal). */
    public EmailSdkMock getEmailSdk() {
        return emailSdk;
    }

    public PushSdkMock getPushSdk() {
        return pushSdk;
    }

    /** Menu de consola sobre los controllers; el login se resuelve con el adapter de identidad. */
    public MenuConsola crearMenu(IdentidadMockAdapter identidad, InputStream entrada, PrintStream salida) {
        Autenticador autenticador = new Autenticador() {
            @Override
            public void iniciarSesion(String userId) {
                identidad.iniciarSesion(userId);
            }

            @Override
            public void cerrarSesion() {
                identidad.cerrarSesion();
            }
        };
        return new MenuConsola(autenticador, perfilController, busquedaController, recomendacionController,
                contratacionController, walletController, notificacionController, entrada, salida);
    }

    /** Libera los recursos de persistencia. */
    public void cerrar() {
        contexto.cerrar();
    }
}
