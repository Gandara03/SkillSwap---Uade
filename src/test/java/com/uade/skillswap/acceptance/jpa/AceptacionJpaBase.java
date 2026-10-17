package com.uade.skillswap.acceptance.jpa;

import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.FranjaCommand;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.bootstrap.Composicion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.infrastructure.identidad.IdentidadMockAdapter;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.presentation.Resultado;
import com.uade.skillswap.support.BasesDePrueba;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Base de los casos de aceptacion JPA: levanta una base NUEVA por test (H2, o MySQL con el perfil mysql; ver
 * BasesDePrueba), con Flyway + hbm2ddl=validate, y arma el sistema completo con {@link Composicion} (repositorios JPA, transacciones, servicios, observer,
 * canales mock y controllers). Los tests solo hablan con los controllers, como lo haria la UI, y por eso
 * obtienen {@code Resultado} en vez de excepciones.
 *
 * <p>Los usuarios disponibles son los de la identidad demo: u-ana, u-bruno, u-cecilia (profesionales) y u-techcorp.
 * El reloj es el del sistema, asi que las franjas se arman relativas a "ahora".
 * Es publica porque tambien la usan los tests de integracion de los controllers (presentation.controller).
 */
public abstract class AceptacionJpaBase {

    protected JpaContexto ctx;
    protected IdentidadMockAdapter identidad;
    protected Composicion app;

    @BeforeEach
    void iniciarAplicacion() {
        ctx = BasesDePrueba.nueva();                 // base nueva y aislada por test (H2 o MySQL)
        identidad = IdentidadMockAdapter.conUsuariosDemo();
        app = Composicion.crear(ctx, identidad, SkillSwapConfig.porDefecto());
    }

    @AfterEach
    void cerrarAplicacion() {
        identidad.cerrarSesion();
        app.cerrar();                                // cierra el EntityManagerFactory y apaga/borra la base
    }

    // ---------- sesion y resultados ----------

    /** Inicia sesion como ese usuario: los controllers toman el actor de la sesion. */
    protected void como(String userId) {
        identidad.iniciarSesion(userId);
    }

    /** Exige exito y devuelve el valor (si fallo, el mensaje del test muestra el error traducido). */
    protected static <T> T ok(Resultado<T> resultado) {
        assertTrue(resultado.esOk(), () -> "Se esperaba exito pero fue: " + resultado);
        return resultado.valor();
    }

    protected static PaginaRequest primeraPagina() {
        return PaginaRequest.de(0, 20);
    }

    protected static BigDecimal sc(long monto) {
        return BigDecimal.valueOf(monto);
    }

    // ---------- perfiles ----------

    /** Registra el perfil PROFESIONAL del usuario demo (el wallet nace con el bono inicial de 100 SC). */
    protected PerfilDto profesional(String userId, String nombre) {
        return registrar(userId, nombre, TipoPerfil.PROFESIONAL, CanalNotificacion.INTERNA, Set.of(), Set.of());
    }

    protected PerfilDto profesional(String userId, String nombre, CanalNotificacion canal) {
        return registrar(userId, nombre, TipoPerfil.PROFESIONAL, canal, Set.of(), Set.of());
    }

    protected PerfilDto registrar(String userId, String nombre, TipoPerfil tipo, CanalNotificacion canal,
                                  Set<String> habilidades, Set<String> intereses) {
        como(userId);
        return ok(app.getPerfilController().registrar(new RegistrarPerfilCommand(userId, nombre, tipo, "UTC", null,
                habilidades, intereses, List.of(), canal)));
    }

    protected BigDecimal saldo(String userId) {
        como(userId);
        return ok(app.getWalletController().consultar()).saldo();
    }

    protected PerfilDto perfilDe(String userId) {
        como(userId);
        return ok(app.getPerfilController().miPerfil());
    }

    // ---------- publicaciones ----------

    /** Una franja de una hora que empieza dentro de {@code desdeAhora}. */
    protected static FranjaCommand franja(Duration desdeAhora) {
        LocalDateTime inicio = LocalDateTime.now().plus(desdeAhora).withNano(0);
        return new FranjaCommand(inicio, inicio.plusHours(1));
    }

    /** Franjas diarias: dentro de 3, 4, 5... dias. */
    protected static List<FranjaCommand> franjasDiarias(int cantidad) {
        List<FranjaCommand> franjas = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            franjas.add(franja(Duration.ofDays(3L + i)));
        }
        return franjas;
    }

    protected static CrearPublicacionCommand mentoria(String titulo, String categoria, long precio, int cupo,
                                                      TipoPoliticaCancelacion politica, Set<String> etiquetas,
                                                      Set<String> habilidades, List<FranjaCommand> franjas) {
        return new CrearPublicacionCommand(TipoPublicacion.MENTORIA, titulo, "Descripcion de " + titulo, categoria,
                Modalidad.REMOTA, 60, cupo, BigDecimal.valueOf(precio), etiquetas, habilidades, politica, franjas);
    }

    /** Crea el borrador y lo publica (sesion del autor). */
    protected PublicacionDto publicar(String userAutor, CrearPublicacionCommand comando) {
        como(userAutor);
        PublicacionDto borrador = ok(app.getPublicacionController().crearBorrador(comando));
        return ok(app.getPublicacionController().publicar(borrador.id()));
    }

    /** Mentoria simple publicada: categoria "programacion", politica FLEXIBLE y {@code cantidadFranjas} franjas diarias. */
    protected PublicacionDto publicarMentoria(String userAutor, long precio, int cupo, int cantidadFranjas) {
        return publicar(userAutor, mentoria("Mentoria de Java", "programacion", precio, cupo,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of("java"), Set.of("java"), franjasDiarias(cantidadFranjas)));
    }

    protected PublicacionDto promocionar(String userAutor, Long publicacionId, TipoPromocion tipo, long valor) {
        como(userAutor);
        return ok(app.getPublicacionController().agregarPromocion(new CrearPromocionCommand(publicacionId,
                "Promo " + tipo + " " + valor, tipo, BigDecimal.valueOf(valor), null, null)));
    }

    protected PublicacionDto publicacionVista(String userAutor, Long publicacionId) {
        como(userAutor);
        return ok(app.getPublicacionController().obtener(publicacionId));
    }

    // ---------- contrataciones ----------

    protected ContratacionDto solicitar(String userCliente, PublicacionDto publicacion, int indiceFranja) {
        como(userCliente);
        return ok(app.getContratacionController().solicitar(new SolicitarContratacionCommand(publicacion.id(),
                publicacion.franjas().get(indiceFranja).id(), "Hola, quiero la mentoria", "Aprender patrones")));
    }

    protected ContratacionDto aceptar(String userProveedor, Long contratacionId) {
        como(userProveedor);
        return ok(app.getContratacionController().aceptar(contratacionId));
    }

    protected ContratacionDto pagar(String userCliente, Long contratacionId) {
        como(userCliente);
        return ok(app.getContratacionController().pagar(contratacionId));
    }

    protected ContratacionDto contratacionVista(String user, Long contratacionId) {
        como(user);
        return ok(app.getContratacionController().obtener(contratacionId));
    }

    /** Solicitar + aceptar + pagar; devuelve la contratacion CONFIRMADA. */
    protected ContratacionDto contratarYPagar(String userProveedor, String userCliente, PublicacionDto publicacion,
                                              int indiceFranja) {
        ContratacionDto solicitud = solicitar(userCliente, publicacion, indiceFranja);
        aceptar(userProveedor, solicitud.id());
        return pagar(userCliente, solicitud.id());
    }

    /** Recorre el flujo completo hasta COMPLETADA. */
    protected ContratacionDto contratarYCompletar(String userProveedor, String userCliente,
                                                  PublicacionDto publicacion, int indiceFranja) {
        ContratacionDto pagada = contratarYPagar(userProveedor, userCliente, publicacion, indiceFranja);
        como(userProveedor);
        ok(app.getContratacionController().iniciar(pagada.id()));
        return ok(app.getContratacionController().completar(pagada.id()));
    }

    protected static CalificarCommand calificacion(Long contratacionId, int puntaje) {
        return new CalificarCommand(contratacionId, puntaje, "Puntaje " + puntaje);
    }
}
