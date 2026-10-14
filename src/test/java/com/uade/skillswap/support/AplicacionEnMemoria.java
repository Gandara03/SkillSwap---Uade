package com.uade.skillswap.support;

import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.notificacion.NotificadorObserver;
import com.uade.skillswap.application.port.in.BusquedaUseCase;
import com.uade.skillswap.application.port.in.CalificacionUseCase;
import com.uade.skillswap.application.port.in.ContratacionUseCase;
import com.uade.skillswap.application.port.in.NotificacionUseCase;
import com.uade.skillswap.application.port.in.PerfilUseCase;
import com.uade.skillswap.application.port.in.PublicacionUseCase;
import com.uade.skillswap.application.port.in.RecomendacionUseCase;
import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.application.port.in.WalletUseCase;
import com.uade.skillswap.application.service.BusquedaService;
import com.uade.skillswap.application.service.CalificacionService;
import com.uade.skillswap.application.service.ContratacionService;
import com.uade.skillswap.application.service.NotificacionService;
import com.uade.skillswap.application.service.PerfilService;
import com.uade.skillswap.application.service.PublicacionService;
import com.uade.skillswap.application.service.RecomendacionService;
import com.uade.skillswap.application.service.SesionService;
import com.uade.skillswap.application.service.WalletService;
import com.uade.skillswap.infrastructure.notificacion.CanalNotificacionFactoryImpl;
import com.uade.skillswap.infrastructure.notificacion.sdk.EmailSdkMock;
import com.uade.skillswap.infrastructure.notificacion.sdk.PushSdkMock;

import java.util.List;

/**
 * Compositor para tests: arma todos los servicios de aplicacion sobre repositorios en memoria, reloj fijo,
 * transaccion simulada, identidad fake y los adapters reales de notificacion (con SDK mock inspeccionables).
 * Es el equivalente de {@code bootstrap.Composicion} pero sin base de datos.
 */
public final class AplicacionEnMemoria {

    // ---- infraestructura fake ----
    public final InMemoryPerfilRepository perfiles = new InMemoryPerfilRepository();
    public final InMemoryPublicacionRepository publicaciones = new InMemoryPublicacionRepository(perfiles);
    public final InMemoryContratacionRepository contrataciones = new InMemoryContratacionRepository();
    public final InMemoryWalletRepository wallets = new InMemoryWalletRepository();
    public final InMemoryCalificacionRepository calificaciones = new InMemoryCalificacionRepository();
    public final InMemoryNotificacionRepository notificaciones = new InMemoryNotificacionRepository();
    public final InMemoryPromocionRepository promociones = new InMemoryPromocionRepository();
    public final TransaccionSimulada transaccion = new TransaccionSimulada();
    public final RelojFijo reloj = new RelojFijo();
    public final IdentidadFake identidad = new IdentidadFake();
    public final EmailSdkMock emailSdk = new EmailSdkMock();
    public final PushSdkMock pushSdk = new PushSdkMock();
    public final SkillSwapConfig config;

    // ---- casos de uso ----
    public final SesionUseCase sesion;
    public final PerfilUseCase perfilUseCase;
    public final PublicacionUseCase publicacionUseCase;
    public final ContratacionUseCase contratacionUseCase;
    public final BusquedaUseCase busquedaUseCase;
    public final RecomendacionUseCase recomendacionUseCase;
    public final CalificacionUseCase calificacionUseCase;
    public final NotificacionUseCase notificacionUseCase;
    public final WalletUseCase walletUseCase;

    private AplicacionEnMemoria(SkillSwapConfig config) {
        this.config = config;
        CanalNotificacionFactoryImpl canales = new CanalNotificacionFactoryImpl(emailSdk, pushSdk, identidad);
        NotificadorObserver notificador = new NotificadorObserver(notificaciones, perfiles, canales, transaccion, reloj);

        this.sesion = new SesionService(identidad, perfiles);
        this.perfilUseCase = new PerfilService(perfiles, wallets, calificaciones, publicaciones, promociones,
                identidad, transaccion, reloj, config);
        this.publicacionUseCase = new PublicacionService(publicaciones, perfiles, promociones, transaccion, reloj);
        this.contratacionUseCase = new ContratacionService(contrataciones, publicaciones, perfiles, wallets,
                promociones, List.of(notificador), transaccion, reloj);
        this.busquedaUseCase = new BusquedaService(publicaciones, perfiles, promociones, reloj);
        this.recomendacionUseCase = new RecomendacionService(publicaciones, perfiles, reloj, promociones, config);
        this.calificacionUseCase = new CalificacionService(calificaciones, contrataciones, perfiles, transaccion, reloj);
        this.notificacionUseCase = new NotificacionService(notificaciones);
        this.walletUseCase = new WalletService(wallets);
    }

    /** Aplicacion con la configuracion por defecto (bono inicial de 100 SC, etc.). */
    public static AplicacionEnMemoria crear() {
        return new AplicacionEnMemoria(SkillSwapConfig.porDefecto());
    }

    public static AplicacionEnMemoria crear(SkillSwapConfig config) {
        return new AplicacionEnMemoria(config);
    }
}
