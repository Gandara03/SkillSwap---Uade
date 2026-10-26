package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.EditarPublicacionCommand;
import com.uade.skillswap.application.command.FranjaCommand;
import com.uade.skillswap.application.dto.HistorialDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.model.publicacion.AccionPublicacion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.query.Pagina;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static com.uade.skillswap.application.exception.ErrorCode.NO_AUTORIZADO;
import static com.uade.skillswap.application.exception.ErrorCode.NO_ENCONTRADO;
import static com.uade.skillswap.application.exception.ErrorCode.REGLA_DE_NEGOCIO;
import static com.uade.skillswap.application.exception.ErrorCode.TRANSICION_INVALIDA;
import static com.uade.skillswap.application.exception.ErrorCode.VALIDACION;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Integracion PublicacionController -> PublicacionUseCase -> JPA/H2, incluido cambiarEstado con sus 5 acciones. */
@DisplayName("PublicacionController (JPA/H2): borrador, edicion, franjas, cambiarEstado y promociones")
class PublicacionControllerTest extends ControllerJpaTestBase {

    private IPublicacionController publicaciones() {
        return app.getPublicacionController();
    }

    /** Borrador completo de Ana (listo para publicar), con dos franjas. Deja la sesion de Ana iniciada. */
    private PublicacionDto borradorDeAna() {
        como("u-ana");
        return ok(publicaciones().crearBorrador(mentoria("Mentoria de Java", "programacion", 50, 2,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of("java"), Set.of("java"), franjasDiarias(2))));
    }

    private static EditarPublicacionCommand edicion(String titulo, int cupo, long precio) {
        return new EditarPublicacionCommand(titulo, "Nueva descripcion", "programacion", Modalidad.PRESENCIAL, 90,
                cupo, BigDecimal.valueOf(precio), Set.of("java", "spring"), Set.of("java"),
                TipoPoliticaCancelacion.ESTRICTA);
    }

    private static CrearPublicacionCommand conNumeros(int duracion, int cupo, BigDecimal precio) {
        return new CrearPublicacionCommand(TipoPublicacion.MENTORIA, "Java", null, "programacion", Modalidad.REMOTA,
                duracion, cupo, precio, Set.of(), Set.of(), null, List.of());
    }

    private static CrearPromocionCommand promocion(Long publicacionId, TipoPromocion tipo, long valor) {
        return new CrearPromocionCommand(publicacionId, "Lanzamiento", tipo, BigDecimal.valueOf(valor), null, null);
    }

    private static List<String> estados(PublicacionDto publicacion) {
        return publicacion.historial().stream().map(HistorialDto::estadoNuevo).toList();
    }

    // ---------- camino feliz ----------

    @Test
    @DisplayName("crear, editar y agregar una franja a un borrador; misPublicaciones lo lista")
    void crearEditarYAgregarFranja() {
        profesional("u-ana", "Ana");
        PublicacionDto borrador = borradorDeAna();

        PublicacionDto editado = ok(publicaciones().editar(borrador.id(), edicion("Java avanzado", 3, 80)));
        FranjaCommand otra = franja(Duration.ofDays(10));
        PublicacionDto conFranja = ok(publicaciones().agregarFranja(borrador.id(), otra.inicio(), otra.fin()));

        assertEquals("Java avanzado", editado.titulo());
        assertEquals(3, editado.cupo());
        assertEquals(Modalidad.PRESENCIAL, editado.modalidad());
        assertEquals(3, conFranja.franjas().size());
        Pagina<PublicacionResumenDto> mias = ok(publicaciones().misPublicaciones(primeraPagina()));
        assertEquals(1L, mias.totalElementos());
        assertEquals("BORRADOR", mias.contenido().get(0).estado());
    }

    @Test
    @DisplayName("cambiarEstado recorre PUBLICAR, PAUSAR, REANUDAR y CERRAR y deja cada paso en el historial")
    void cambiarEstadoRecorreLasAcciones() {
        profesional("u-ana", "Ana");
        Long id = borradorDeAna().id();

        assertEquals("PUBLICADA", ok(publicaciones().cambiarEstado(id, AccionPublicacion.PUBLICAR, null)).estado());
        assertEquals("PAUSADA", ok(publicaciones().cambiarEstado(id, AccionPublicacion.PAUSAR, null)).estado());
        assertEquals("PUBLICADA", ok(publicaciones().cambiarEstado(id, AccionPublicacion.REANUDAR, null)).estado());
        PublicacionDto cerrada = ok(publicaciones().cambiarEstado(id, AccionPublicacion.CERRAR, null));

        assertEquals(List.of("BORRADOR", "PUBLICADA", "PAUSADA", "PUBLICADA", "CERRADA"), estados(cerrada));
        assertEquals("CERRADA", publicacionVista("u-ana", id).estado(), "el estado quedo persistido");
    }

    @Test
    @DisplayName("cambiarEstado CANCELAR exige un motivo y lo guarda en el historial")
    void cambiarEstadoCancelarConMotivo() {
        profesional("u-ana", "Ana");
        Long id = borradorDeAna().id();
        ok(publicaciones().cambiarEstado(id, AccionPublicacion.PUBLICAR, null));

        falla(VALIDACION, publicaciones().cambiarEstado(id, AccionPublicacion.CANCELAR, "  "));
        assertEquals("PUBLICADA", publicacionVista("u-ana", id).estado());

        PublicacionDto cancelada = ok(publicaciones().cambiarEstado(id, AccionPublicacion.CANCELAR, "Ya no la dicto"));
        assertEquals("CANCELADA", cancelada.estado());
        assertEquals("Ya no la dicto", cancelada.historial().get(cancelada.historial().size() - 1).motivo());
    }

    @Test
    @DisplayName("agregarPromocion baja el precio final; un monto fijo mayor a 100 vale (el tope es del porcentaje)")
    void agregarPromocion() {
        profesional("u-ana", "Ana");
        PublicacionDto publicada = publicarMentoria("u-ana", 50, 2, 1);
        como("u-ana");

        PublicacionDto conPorcentaje = ok(publicaciones().agregarPromocion(
                promocion(publicada.id(), TipoPromocion.PORCENTAJE, 20)));
        PublicacionDto gratis = ok(publicaciones().agregarPromocion(
                promocion(publicada.id(), TipoPromocion.MONTO_FIJO, 150)));

        assertEquals(0, new BigDecimal("40").compareTo(conPorcentaje.precioFinal()));
        assertEquals(0, BigDecimal.ZERO.compareTo(gratis.precioFinal()));
    }

    // ---------- validacion de entrada ----------

    @Test
    @DisplayName("crearBorrador con datos imposibles es VALIDACION y no guarda nada")
    void crearBorradorInvalido() {
        profesional("u-ana", "Ana");
        como("u-ana");
        LocalDateTime inicio = LocalDateTime.now().plusDays(3).withNano(0);
        FranjaCommand invertida = new FranjaCommand(inicio, inicio.minusHours(1));

        falla(VALIDACION, publicaciones().crearBorrador(null));
        falla(VALIDACION, publicaciones().crearBorrador(new CrearPublicacionCommand(null, "Java", null, null, null,
                60, 1, null, Set.of(), Set.of(), null, List.of())));
        falla(VALIDACION, publicaciones().crearBorrador(conNumeros(-60, 1, null)));
        falla(VALIDACION, publicaciones().crearBorrador(conNumeros(60, -1, null)));
        falla(VALIDACION, publicaciones().crearBorrador(conNumeros(60, 1, BigDecimal.valueOf(-10))));
        falla(VALIDACION, publicaciones().crearBorrador(mentoria(texto(201), "programacion", 50, 1,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of(), Set.of(), List.of())));
        falla(VALIDACION, publicaciones().crearBorrador(mentoria("Java", "programacion", 50, 1,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of(" "), Set.of(), List.of())));
        falla(VALIDACION, publicaciones().crearBorrador(mentoria("Java", "programacion", 50, 1,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of(), Set.of(), List.of(invertida))));

        assertEquals(0L, ok(publicaciones().misPublicaciones(primeraPagina())).totalElementos());
    }

    @Test
    @DisplayName("un borrador incompleto (sin titulo, categoria ni precio) es una entrada valida")
    void borradorIncompletoEsValido() {
        profesional("u-ana", "Ana");
        como("u-ana");

        PublicacionDto borrador = ok(publicaciones().crearBorrador(new CrearPublicacionCommand(
                TipoPublicacion.MENTORIA, null, null, null, null, 0, 0, null, null, null, null, null)));

        assertEquals("BORRADOR", borrador.estado());
    }

    @Test
    @DisplayName("editar, agregarFranja, cambiarEstado, obtener y misPublicaciones validan su entrada sin tocar nada")
    void operacionesSobreUnBorradorInvalidas() {
        profesional("u-ana", "Ana");
        Long id = borradorDeAna().id();
        LocalDateTime inicio = LocalDateTime.now().plusDays(5).withNano(0);

        falla(VALIDACION, publicaciones().editar(null, edicion("Otro", 1, 10)));
        falla(VALIDACION, publicaciones().editar(id, null));
        falla(VALIDACION, publicaciones().editar(id, edicion("Otro", -1, 10)));
        falla(VALIDACION, publicaciones().agregarFranja(id, null, inicio));
        falla(VALIDACION, publicaciones().agregarFranja(id, inicio, inicio));
        falla(VALIDACION, publicaciones().cambiarEstado(id, null, null));
        falla(VALIDACION, publicaciones().cambiarEstado(0L, AccionPublicacion.PUBLICAR, null));
        falla(VALIDACION, publicaciones().cancelar(id, texto(1001)));
        falla(VALIDACION, publicaciones().obtener(-1L));
        falla(VALIDACION, publicaciones().misPublicaciones(null));

        PublicacionDto igual = publicacionVista("u-ana", id);
        assertEquals("Mentoria de Java", igual.titulo());
        assertEquals(2, igual.franjas().size());
        assertEquals("BORRADOR", igual.estado());
    }

    @Test
    @DisplayName("agregarPromocion valida descripcion, tipo, valor, tope del porcentaje y vigencia")
    void agregarPromocionInvalida() {
        profesional("u-ana", "Ana");
        PublicacionDto publicada = publicarMentoria("u-ana", 50, 2, 1);
        como("u-ana");
        LocalDateTime hoy = LocalDateTime.now();

        falla(VALIDACION, publicaciones().agregarPromocion(null));
        falla(VALIDACION, publicaciones().agregarPromocion(promocion(null, TipoPromocion.PORCENTAJE, 10)));
        falla(VALIDACION, publicaciones().agregarPromocion(promocion(publicada.id(), null, 10)));
        falla(VALIDACION, publicaciones().agregarPromocion(promocion(publicada.id(), TipoPromocion.PORCENTAJE, 0)));
        falla(VALIDACION, publicaciones().agregarPromocion(promocion(publicada.id(), TipoPromocion.PORCENTAJE, 101)));
        falla(VALIDACION, publicaciones().agregarPromocion(new CrearPromocionCommand(publicada.id(), " ",
                TipoPromocion.PORCENTAJE, BigDecimal.TEN, null, null)));
        falla(VALIDACION, publicaciones().agregarPromocion(new CrearPromocionCommand(publicada.id(), "Al reves",
                TipoPromocion.PORCENTAJE, BigDecimal.TEN, hoy.plusDays(5), hoy)));

        assertEquals(0, new BigDecimal("50").compareTo(publicacionVista("u-ana", publicada.id()).precioFinal()),
                "ninguna promocion se guardo");
    }

    // ---------- permisos ----------

    @Test
    @DisplayName("otro usuario no puede editar, agregar franjas, cambiar estado, promocionar ni ver un borrador ajeno")
    void usuarioAjenoNoAutorizado() {
        profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        PublicacionDto borrador = borradorDeAna();
        FranjaCommand otra = franja(Duration.ofDays(10));
        como("u-bruno");

        falla(NO_AUTORIZADO, publicaciones().editar(borrador.id(), edicion("Ahora es mia", 1, 10)));
        falla(NO_AUTORIZADO, publicaciones().agregarFranja(borrador.id(), otra.inicio(), otra.fin()));
        falla(NO_AUTORIZADO, publicaciones().cambiarEstado(borrador.id(), AccionPublicacion.PUBLICAR, null));
        falla(NO_AUTORIZADO, publicaciones().cambiarEstado(borrador.id(), AccionPublicacion.CANCELAR, "Porque si"));
        falla(NO_AUTORIZADO, publicaciones().agregarPromocion(
                promocion(borrador.id(), TipoPromocion.PORCENTAJE, 10)));
        falla(NO_AUTORIZADO, publicaciones().obtener(borrador.id()));

        PublicacionDto igual = publicacionVista("u-ana", borrador.id());
        assertEquals("BORRADOR", igual.estado());
        assertEquals("Mentoria de Java", igual.titulo());
        assertEquals(2, igual.franjas().size());
    }

    @Test
    @DisplayName("sin sesion no se puede crear, listar ni cambiar el estado (la sesion se mira antes que la entrada)")
    void sinSesionNoAutorizado() {
        sinSesion();

        falla(NO_AUTORIZADO, publicaciones().crearBorrador(mentoria("Java", "programacion", 50, 1,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of(), Set.of(), List.of())));
        falla(NO_AUTORIZADO, publicaciones().misPublicaciones(primeraPagina()));
        falla(NO_AUTORIZADO, publicaciones().cambiarEstado(1L, null, null));
    }

    // ---------- otros codigos ----------

    @Test
    @DisplayName("una transicion invalida es TRANSICION_INVALIDA y una publicacion inexistente NO_ENCONTRADO")
    void transicionInvalidaYNoEncontrada() {
        profesional("u-ana", "Ana");
        Long id = borradorDeAna().id();

        falla(TRANSICION_INVALIDA, publicaciones().cambiarEstado(id, AccionPublicacion.REANUDAR, null));
        ok(publicaciones().cambiarEstado(id, AccionPublicacion.PUBLICAR, null));
        falla(TRANSICION_INVALIDA, publicaciones().cambiarEstado(id, AccionPublicacion.REANUDAR, null));
        falla(NO_ENCONTRADO, publicaciones().cambiarEstado(999L, AccionPublicacion.PUBLICAR, null));
        falla(NO_ENCONTRADO, publicaciones().obtener(999L));
    }

    @Test
    @DisplayName("una organizacion no puede crear una mentoria: REGLA_DE_NEGOCIO (la decide el dominio)")
    void organizacionNoCreaMentoria() {
        registrar("u-techcorp", "TechCorp", TipoPerfil.ORGANIZACION, CanalNotificacion.INTERNA, Set.of(), Set.of());

        falla(REGLA_DE_NEGOCIO, publicaciones().crearBorrador(mentoria("Java", "programacion", 50, 1,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of(), Set.of(), List.of())));
    }
}
