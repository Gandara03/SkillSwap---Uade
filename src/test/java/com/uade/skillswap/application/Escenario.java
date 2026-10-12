package com.uade.skillswap.application;

import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.FranjaCommand;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.NotificacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.support.AplicacionEnMemoria;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fixture de los tests de aplicacion: arma perfiles, publicaciones y recorridos de una contratacion usando
 * SOLO los casos de uso de {@link AplicacionEnMemoria} (nada se inserta por la puerta de atras), asi que
 * cada escenario es tambien una prueba de integracion de los servicios con los fakes.
 */
public final class Escenario {

    /** Aplicacion completa en memoria; el reloj arranca fijo en 2026-03-02 10:00. */
    public final AplicacionEnMemoria app;

    public Escenario() {
        this(AplicacionEnMemoria.crear());
    }

    public Escenario(AplicacionEnMemoria app) {
        this.app = app;
    }

    // ---------- perfiles ----------

    public PerfilDto profesional(String userId, String nombre) {
        return registrar(userId, nombre, TipoPerfil.PROFESIONAL, CanalNotificacion.INTERNA, Set.of("java"), Set.of("java"));
    }

    public PerfilDto profesional(String userId, String nombre, CanalNotificacion canal) {
        return registrar(userId, nombre, TipoPerfil.PROFESIONAL, canal, Set.of("java"), Set.of("java"));
    }

    public PerfilDto organizacion(String userId, String nombre) {
        return registrar(userId, nombre, TipoPerfil.ORGANIZACION, CanalNotificacion.INTERNA, Set.of(), Set.of());
    }

    /** Da de alta el usuario en la identidad fake, inicia su sesion y registra su perfil (con wallet y bono). */
    public PerfilDto registrar(String userId, String nombre, TipoPerfil tipo, CanalNotificacion canal,
                               Set<String> habilidades, Set<String> intereses) {
        app.identidad.agregarUsuario(userId, nombre);
        app.identidad.iniciarSesion(userId);
        return app.perfilUseCase.registrar(new RegistrarPerfilCommand(userId, nombre, tipo, "UTC", null,
                habilidades, intereses, List.of(), canal));
    }

    // ---------- publicaciones ----------

    /** Descripcion de una oferta con valores razonables por defecto (mentoria de 40 SC, cupo 1, dos franjas). */
    public static final class Oferta {
        TipoPublicacion tipo = TipoPublicacion.MENTORIA;
        String titulo = "Mentoria de Java";
        String descripcion = "Clean Architecture y patrones";
        String categoria = "programacion";
        Modalidad modalidad = Modalidad.REMOTA;
        int cupo = 1;
        String precio = "40";
        Set<String> etiquetas = Set.of("java", "backend");
        Set<String> habilidades = Set.of("java");
        TipoPoliticaCancelacion politica = TipoPoliticaCancelacion.FLEXIBLE;
        List<Long> horasFranjas = List.of(100L, 130L);

        public Oferta tipo(TipoPublicacion v) { this.tipo = v; return this; }
        public Oferta titulo(String v) { this.titulo = v; return this; }
        public Oferta descripcion(String v) { this.descripcion = v; return this; }
        public Oferta categoria(String v) { this.categoria = v; return this; }
        public Oferta modalidad(Modalidad v) { this.modalidad = v; return this; }
        public Oferta cupo(int v) { this.cupo = v; return this; }
        public Oferta precio(String v) { this.precio = v; return this; }
        public Oferta etiquetas(String... v) { this.etiquetas = Set.of(v); return this; }
        public Oferta habilidades(String... v) { this.habilidades = Set.of(v); return this; }
        public Oferta politica(TipoPoliticaCancelacion v) { this.politica = v; return this; }
        /** Horas, contadas desde el reloj actual, a las que arranca cada franja (dura 1 hora). */
        public Oferta franjasEn(Long... horas) { this.horasFranjas = List.of(horas); return this; }
        public Oferta sinFranjas() { this.horasFranjas = List.of(); return this; }
    }

    public Oferta oferta() {
        return new Oferta();
    }

    public FranjaCommand franjaEn(long horasDesdeAhora) {
        LocalDateTime inicio = app.reloj.ahora().plusHours(horasDesdeAhora);
        return new FranjaCommand(inicio, inicio.plusHours(1));
    }

    public CrearPublicacionCommand comando(Oferta o) {
        List<FranjaCommand> franjas = new ArrayList<>();
        for (Long h : o.horasFranjas) {
            franjas.add(franjaEn(h));
        }
        return new CrearPublicacionCommand(o.tipo, o.titulo, o.descripcion, o.categoria, o.modalidad, 60, o.cupo,
                new BigDecimal(o.precio), o.etiquetas, o.habilidades, o.politica, franjas);
    }

    public PublicacionDto borrador(PerfilDto autor, Oferta o) {
        return app.publicacionUseCase.crearBorrador(autor.id(), comando(o));
    }

    public PublicacionDto publicada(PerfilDto autor, Oferta o) {
        PublicacionDto borrador = borrador(autor, o);
        return app.publicacionUseCase.publicar(autor.id(), borrador.id());
    }

    public PublicacionDto publicada(PerfilDto autor) {
        return publicada(autor, oferta());
    }

    // ---------- recorrido de una contratacion ----------

    public Long franjaId(PublicacionDto publicacion, int indice) {
        return publicacion.franjas().get(indice).id();
    }

    public ContratacionDto solicitar(PerfilDto cliente, PublicacionDto publicacion, int indiceFranja) {
        return app.contratacionUseCase.solicitar(cliente.id(), new SolicitarContratacionCommand(
                publicacion.id(), franjaId(publicacion, indiceFranja), "Hola, quiero la sesion", "Aprender patrones"));
    }

    public ContratacionDto aceptada(PerfilDto cliente, PerfilDto proveedor, PublicacionDto publicacion, int indiceFranja) {
        ContratacionDto solicitud = solicitar(cliente, publicacion, indiceFranja);
        return app.contratacionUseCase.aceptar(proveedor.id(), solicitud.id());
    }

    public ContratacionDto pagada(PerfilDto cliente, PerfilDto proveedor, PublicacionDto publicacion, int indiceFranja) {
        ContratacionDto aceptada = aceptada(cliente, proveedor, publicacion, indiceFranja);
        return app.contratacionUseCase.pagar(cliente.id(), aceptada.id());
    }

    public ContratacionDto enCurso(PerfilDto cliente, PerfilDto proveedor, PublicacionDto publicacion, int indiceFranja) {
        ContratacionDto pagada = pagada(cliente, proveedor, publicacion, indiceFranja);
        return app.contratacionUseCase.iniciar(proveedor.id(), pagada.id());
    }

    public ContratacionDto completada(PerfilDto cliente, PerfilDto proveedor, PublicacionDto publicacion, int indiceFranja) {
        ContratacionDto enCurso = enCurso(cliente, proveedor, publicacion, indiceFranja);
        return app.contratacionUseCase.completar(proveedor.id(), enCurso.id());
    }

    /** El cliente califica al proveedor de una contratacion ya completada. */
    public void calificar(PerfilDto autor, ContratacionDto completada, int puntaje) {
        app.calificacionUseCase.calificar(autor.id(), new CalificarCommand(completada.id(), puntaje, "comentario"));
    }

    // ---------- consultas y utilidades ----------

    public BigDecimal saldo(PerfilDto perfil) {
        return app.walletUseCase.consultar(perfil.id()).saldo();
    }

    public List<NotificacionDto> bandeja(PerfilDto perfil) {
        return app.notificacionUseCase.listar(perfil.id(), false, PaginaRequest.de(0, PaginaRequest.TAMANO_MAXIMO))
                .contenido();
    }

    public ContratacionDto estado(PerfilDto actor, ContratacionDto contratacion) {
        return app.contratacionUseCase.obtener(actor.id(), contratacion.id());
    }

    public PublicacionDto publicacion(PerfilDto actor, PublicacionDto publicacion) {
        return app.publicacionUseCase.obtener(actor.id(), publicacion.id());
    }

    public void avanzarHoras(long horas) {
        app.reloj.avanzar(Duration.ofHours(horas));
    }

    /** Compara montos por valor (no por escala): "40" == 40.00. */
    public static void assertMonto(String esperado, BigDecimal real) {
        assertTrue(real != null && new BigDecimal(esperado).compareTo(real) == 0,
                "se esperaba " + esperado + " pero fue " + real);
    }
}
