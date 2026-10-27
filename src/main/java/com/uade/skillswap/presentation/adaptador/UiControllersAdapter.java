package com.uade.skillswap.presentation.adaptador;

import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PerfilPublicoDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.dto.ResultadoCancelacionDto;
import com.uade.skillswap.application.exception.ErrorCode;
import com.uade.skillswap.domain.model.publicacion.AccionPublicacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;
import com.uade.skillswap.presentation.controller.IBusquedaController;
import com.uade.skillswap.presentation.controller.ICalificacionController;
import com.uade.skillswap.presentation.controller.IContratacionController;
import com.uade.skillswap.presentation.controller.INotificacionController;
import com.uade.skillswap.presentation.controller.IPerfilController;
import com.uade.skillswap.presentation.controller.IPublicacionController;
import com.uade.skillswap.presentation.controller.IRecomendacionController;
import com.uade.skillswap.presentation.controller.IWalletController;
import com.uade.skillswap.presentation.ui.Autenticador;
import com.uade.skillswap.presentation.ui.swing.contrato.Models;
import com.uade.skillswap.presentation.ui.swing.contrato.OperacionFallidaException;
import com.uade.skillswap.presentation.ui.swing.contrato.UiControllers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Adapter: implementa el contrato que usa la pantalla ({@link UiControllers}, el Target) delegando en los
 * controllers de la aplicacion (los Adaptados, que no cambian). Concentra todas las diferencias entre los dos:
 * inicia la sesion del userId que manda la pantalla, convierte ids, enums y paginas (con {@link MapeadorUi}) y
 * convierte un {@link Resultado} con error en {@link OperacionFallidaException}.
 */
public class UiControllersAdapter implements UiControllers, UiControllers.BusquedaController,
        UiControllers.PublicacionController, UiControllers.ContratacionController,
        UiControllers.CalificacionController, UiControllers.WalletController,
        UiControllers.NotificacionController, UiControllers.PerfilController {

    // La pantalla muestra listas sin paginar: se pide la primera pagina.
    private static final PaginaRequest PAGINA = PaginaRequest.de(0, 50);
    private static final PaginaRequest RECOMENDACIONES = PaginaRequest.de(0, 6);
    private static final PaginaRequest MAXIMA = PaginaRequest.de(0, PaginaRequest.TAMANO_MAXIMO);

    private final Autenticador autenticador;
    private final IPerfilController perfiles;
    private final IPublicacionController publicaciones;
    private final IBusquedaController busquedas;
    private final IRecomendacionController recomendaciones;
    private final IContratacionController contrataciones;
    private final ICalificacionController calificaciones;
    private final INotificacionController notificaciones;
    private final IWalletController wallet;

    public UiControllersAdapter(Autenticador autenticador, IPerfilController perfiles,
                                IPublicacionController publicaciones, IBusquedaController busquedas,
                                IRecomendacionController recomendaciones, IContratacionController contrataciones,
                                ICalificacionController calificaciones, INotificacionController notificaciones,
                                IWalletController wallet) {
        this.autenticador = autenticador;
        this.perfiles = perfiles;
        this.publicaciones = publicaciones;
        this.busquedas = busquedas;
        this.recomendaciones = recomendaciones;
        this.contrataciones = contrataciones;
        this.calificaciones = calificaciones;
        this.notificaciones = notificaciones;
        this.wallet = wallet;
    }

    @Override
    public BusquedaController busqueda() {
        return this;
    }

    @Override
    public PublicacionController publicaciones() {
        return this;
    }

    @Override
    public ContratacionController contrataciones() {
        return this;
    }

    @Override
    public CalificacionController calificaciones() {
        return this;
    }

    @Override
    public WalletController wallet() {
        return this;
    }

    @Override
    public NotificacionController notificaciones() {
        return this;
    }

    @Override
    public PerfilController perfiles() {
        return this;
    }

    // ---------- busqueda ----------

    @Override
    public List<Models.Publicacion> buscar(Models.FiltrosBusqueda filtros) {
        FiltroBusqueda filtro = filtros == null ? FiltroBusqueda.vacio()
                : new FiltroBusqueda(vacioComoNulo(filtros.texto()), categoria(filtros.categoria()), null,
                        MapeadorUi.convertir(filtros.modalidad(), Modalidad.class), null, null, null, null, null, null,
                        MapeadorUi.criterio(filtros.orden()));
        return valor(busquedas.buscar(filtro, PAGINA)).contenido().stream().map(MapeadorUi::publicacion).toList();
    }

    @Override
    public List<Models.Publicacion> recomendar(String userId) {
        iniciarSesion(userId);
        // Sin estrategia, la aplicacion usa la de afinidad.
        return valor(recomendaciones.recomendar(null, RECOMENDACIONES)).contenido().stream()
                .map(MapeadorUi::publicacion).toList();
    }

    /** No hay un catalogo de categorias: se toman las de las publicaciones publicadas. */
    @Override
    public List<String> categorias() {
        List<String> categorias = new ArrayList<>();
        categorias.add(TODAS_LAS_CATEGORIAS);
        valor(busquedas.buscar(FiltroBusqueda.vacio(), MAXIMA)).contenido().stream()
                .map(PublicacionResumenDto::categoria)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(categorias::add);
        return categorias;
    }

    // ---------- publicaciones ----------

    @Override
    public List<Models.Publicacion> listarPropias(String userId) {
        iniciarSesion(userId);
        return valor(publicaciones.misPublicaciones(PAGINA)).contenido().stream()
                .map(MapeadorUi::publicacion).toList();
    }

    @Override
    public Models.Publicacion crearBorrador(String userId, Models.NuevaPublicacion datos) {
        iniciarSesion(userId);
        PublicacionDto creada = valor(publicaciones.crearBorrador(MapeadorUi.comando(datos)));
        return MapeadorUi.publicacion(creada, valor(perfiles.miPerfil()));
    }

    @Override
    public Models.Publicacion cambiarEstado(String userId, String publicacionId, Models.AccionPublicacion accion,
                                            String motivo) {
        iniciarSesion(userId);
        PublicacionDto modificada = valor(publicaciones.cambiarEstado(id(publicacionId, "la publicacion"),
                MapeadorUi.convertir(accion, AccionPublicacion.class), motivo));
        return MapeadorUi.publicacion(modificada, valor(perfiles.miPerfil()));
    }

    @Override
    public List<Models.Franja> franjasDisponibles(String userId, String publicacionId) {
        iniciarSesion(userId);
        return valor(publicaciones.obtener(id(publicacionId, "la publicacion"))).franjas().stream()
                .filter(f -> !f.ocupada())
                .map(MapeadorUi::franja)
                .toList();
    }

    // ---------- contrataciones ----------

    @Override
    public List<Models.Contratacion> listarContrataciones(String userId) {
        iniciarSesion(userId);
        Long yo = valor(perfiles.miPerfil()).id();
        Map<Long, String> nombres = new HashMap<>(); // un pedido por contraparte distinta
        return valor(contrataciones.misContrataciones(PAGINA)).contenido().stream()
                .map(c -> vista(c, yo, nombres))
                .toList();
    }

    @Override
    public Models.Contratacion solicitar(String userId, String publicacionId, String franjaId, String mensaje,
                                         String objetivo) {
        iniciarSesion(userId);
        return vista(valor(contrataciones.solicitar(new SolicitarContratacionCommand(id(publicacionId,
                "la publicacion"), id(franjaId, "la franja"), mensaje, objetivo))));
    }

    @Override
    public Models.Contratacion aceptar(String userId, String contratacionId) {
        iniciarSesion(userId);
        return vista(valor(contrataciones.aceptar(id(contratacionId, "la contratacion"))));
    }

    @Override
    public Models.Contratacion rechazar(String userId, String contratacionId, String motivo) {
        iniciarSesion(userId);
        return vista(valor(contrataciones.rechazar(id(contratacionId, "la contratacion"), motivo)));
    }

    @Override
    public Models.Contratacion pagar(String userId, String contratacionId) {
        iniciarSesion(userId);
        return vista(valor(contrataciones.pagar(id(contratacionId, "la contratacion"))));
    }

    @Override
    public Models.Contratacion iniciar(String userId, String contratacionId) {
        iniciarSesion(userId);
        return vista(valor(contrataciones.iniciar(id(contratacionId, "la contratacion"))));
    }

    @Override
    public Models.Contratacion completar(String userId, String contratacionId) {
        iniciarSesion(userId);
        return vista(valor(contrataciones.completar(id(contratacionId, "la contratacion"))));
    }

    @Override
    public Models.ResultadoCancelacion cancelar(String userId, String contratacionId, String motivo) {
        iniciarSesion(userId);
        ResultadoCancelacionDto r = valor(contrataciones.cancelar(id(contratacionId, "la contratacion"), motivo));
        return new Models.ResultadoCancelacion(vista(r.contratacion()), r.politica(), r.reintegroCliente(),
                r.compensacionProveedor());
    }

    /** La contratacion vista desde el usuario activo: la contraparte se muestra con su rol. */
    private Models.Contratacion vista(ContratacionDto c) {
        return vista(c, valor(perfiles.miPerfil()).id(), new HashMap<>());
    }

    private Models.Contratacion vista(ContratacionDto c, Long yo, Map<Long, String> nombres) {
        boolean soyCliente = yo.equals(c.clienteId());
        Long otro = soyCliente ? c.proveedorId() : c.clienteId();
        String contraparte = (soyCliente ? "Proveedor: " : "Cliente: ") + nombres.computeIfAbsent(otro, this::nombreDe);
        return MapeadorUi.contratacion(c, contraparte);
    }

    private String nombreDe(Long perfilId) {
        Resultado<PerfilPublicoDto> perfil = perfiles.verPerfilPublico(perfilId);
        return perfil.esOk() ? perfil.valor().nombreVisible() : "perfil #" + perfilId;
    }

    // ---------- calificaciones ----------

    @Override
    public void calificar(String userId, String contratacionId, int puntaje, String comentario) {
        iniciarSesion(userId);
        valor(calificaciones.calificar(new CalificarCommand(id(contratacionId, "la contratacion"), puntaje,
                comentario)));
    }

    // ---------- wallet ----------

    @Override
    public BigDecimal consultarSaldo(String userId) {
        iniciarSesion(userId);
        return valor(wallet.consultar()).saldo();
    }

    @Override
    public List<Models.Movimiento> listarMovimientos(String userId) {
        iniciarSesion(userId);
        return valor(wallet.movimientos(PAGINA)).contenido().stream().map(MapeadorUi::movimiento).toList();
    }

    // ---------- notificaciones ----------

    @Override
    public List<Models.Notificacion> listarNotificaciones(String userId) {
        iniciarSesion(userId);
        return valor(notificaciones.listar(false, PAGINA)).contenido().stream()
                .map(MapeadorUi::notificacion).toList();
    }

    @Override
    public void marcarLeida(String userId, String notificacionId) {
        iniciarSesion(userId);
        valor(notificaciones.marcarLeida(id(notificacionId, "la notificacion")));
    }

    /** No hay un caso de uso para marcar todas: se marcan de a una, pidiendo las no leidas hasta que no quede ninguna. */
    @Override
    public void marcarTodasLeidas(String userId) {
        iniciarSesion(userId);
        List<NotificacionDto> pendientes = valor(notificaciones.listar(true, MAXIMA)).contenido();
        while (!pendientes.isEmpty()) {
            for (NotificacionDto notificacion : pendientes) {
                valor(notificaciones.marcarLeida(notificacion.id()));
            }
            pendientes = valor(notificaciones.listar(true, MAXIMA)).contenido();
        }
    }

    // ---------- perfil ----------

    /** Datos propios del perfil, mas reputacion y ultimas calificaciones tal como las arma la vista publica. */
    @Override
    public Models.Perfil verPerfil(String userId) {
        iniciarSesion(userId);
        PerfilDto yo = valor(perfiles.miPerfil());
        PerfilPublicoDto publico = valor(perfiles.verPerfilPublico(yo.id()));
        return MapeadorUi.perfil(yo, publico, completadasComoProveedor(yo.id()));
    }

    private int completadasComoProveedor(Long perfilId) {
        int completadas = 0;
        int numero = 0;
        Pagina<ContratacionDto> pagina;
        do {
            pagina = valor(contrataciones.misContrataciones(PaginaRequest.de(numero, PaginaRequest.TAMANO_MAXIMO)));
            for (ContratacionDto c : pagina.contenido()) {
                if ("COMPLETADA".equals(c.estado()) && perfilId.equals(c.proveedorId())) {
                    completadas++;
                }
            }
            numero++;
        } while (pagina.tieneSiguiente());
        return completadas;
    }

    // ---------- soporte ----------

    /** Login simulado: cada operacion corre con la sesion del userId que manda la pantalla. */
    private void iniciarSesion(String userId) {
        try {
            autenticador.iniciarSesion(userId);
        } catch (IllegalArgumentException e) {
            throw new OperacionFallidaException(ErrorCode.NO_AUTORIZADO.name(),
                    "No existe el usuario '" + userId + "'.");
        }
    }

    /** El valor de una operacion exitosa; si fallo, su codigo y su mensaje (ya traducidos por el controller). */
    private static <T> T valor(Resultado<T> resultado) {
        if (resultado.esError()) {
            throw new OperacionFallidaException(resultado.codigo().name(), resultado.mensaje());
        }
        return resultado.valor();
    }

    /** Ids de texto a numero. Si falta, se pasa null y el controller avisa que es obligatorio. */
    private static Long id(String texto, String nombre) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(texto.trim());
        } catch (NumberFormatException e) {
            throw new OperacionFallidaException(ErrorCode.VALIDACION.name(),
                    "Formato invalido: " + nombre + " debe ser un id numerico");
        }
    }

    private static String vacioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static String categoria(String categoria) {
        return TODAS_LAS_CATEGORIAS.equals(categoria) ? null : vacioComoNulo(categoria);
    }
}
