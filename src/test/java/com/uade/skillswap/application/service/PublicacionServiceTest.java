package com.uade.skillswap.application.service;

import com.uade.skillswap.application.Escenario;
import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.EditarPublicacionCommand;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.exception.RecursoNoEncontradoException;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.exception.TransicionInvalidaException;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.support.AplicacionEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PublicacionService: ciclo de vida, permisos del autor, franjas y promociones")
class PublicacionServiceTest {

    private Escenario e;
    private AplicacionEnMemoria app;
    private PerfilDto ana;
    private PerfilDto bruno;
    private PerfilDto techcorp;

    @BeforeEach
    void preparar() {
        e = new Escenario();
        app = e.app;
        ana = e.profesional("u-ana", "Ana");
        bruno = e.profesional("u-bruno", "Bruno");
        techcorp = e.organizacion("u-techcorp", "TechCorp");
    }

    private EditarPublicacionCommand edicion(String titulo) {
        return new EditarPublicacionCommand(titulo, "Descripcion nueva", "arquitectura", Modalidad.PRESENCIAL, 90, 3,
                new BigDecimal("55"), Set.of("ddd"), Set.of("DDD"), TipoPoliticaCancelacion.ESTRICTA);
    }

    // ---------- crear ----------

    @Test
    @DisplayName("crearBorrador guarda un BORRADOR del autor con sus franjas, precio final igual al base y historial inicial")
    void crearBorrador() {
        PublicacionDto borrador = e.borrador(ana, e.oferta());

        assertNotNull(borrador.id());
        assertEquals("BORRADOR", borrador.estado());
        assertEquals(ana.id(), borrador.autorId());
        assertEquals(2, borrador.franjas().size());
        assertTrue(borrador.franjas().stream().allMatch(f -> f.id() != null && !f.ocupada()));
        Escenario.assertMonto("40", borrador.precioBase());
        Escenario.assertMonto("40", borrador.precioFinal());
        assertEquals(1, borrador.cupoDisponible());
        assertEquals(1, borrador.historial().size());
        assertNull(borrador.publicadaEn());
        assertEquals(Set.of("java"), borrador.habilidades());
    }

    @Test
    @DisplayName("un borrador puede estar incompleto: la completitud se exige recien al publicar")
    void borradorIncompleto() {
        CrearPublicacionCommand vacio = new CrearPublicacionCommand(TipoPublicacion.MENTORIA, null, null, null, null,
                0, 0, null, null, null, null, null);

        PublicacionDto borrador = app.publicacionUseCase.crearBorrador(ana.id(), vacio);

        assertEquals("BORRADOR", borrador.estado());
        assertNull(borrador.precioFinal());
        assertThrows(ReglaDeNegocioException.class, () -> app.publicacionUseCase.publicar(ana.id(), borrador.id()));
    }

    @Test
    @DisplayName("solo una organizacion publica desafios y solo un profesional publica mentorias o servicios")
    void tipoSegunPerfil() {
        PublicacionDto desafio = e.borrador(techcorp, e.oferta().tipo(TipoPublicacion.DESAFIO));
        assertEquals(TipoPublicacion.DESAFIO, desafio.tipo());

        assertThrows(ReglaDeNegocioException.class, () -> e.borrador(ana, e.oferta().tipo(TipoPublicacion.DESAFIO)));
        assertThrows(ReglaDeNegocioException.class, () -> e.borrador(techcorp, e.oferta()));
    }

    @Test
    @DisplayName("crear con perfil inexistente es RecursoNoEncontrado; con actor o comando nulos es ValidacionException")
    void crearEntradasInvalidas() {
        assertThrows(RecursoNoEncontradoException.class,
                () -> app.publicacionUseCase.crearBorrador(999L, e.comando(e.oferta())));
        assertThrows(ValidacionException.class, () -> app.publicacionUseCase.crearBorrador(null, e.comando(e.oferta())));
        assertThrows(ValidacionException.class, () -> app.publicacionUseCase.crearBorrador(ana.id(), null));
    }

    @Test
    @DisplayName("una franja en el pasado o con fin anterior al inicio se rechaza al crear")
    void franjasInvalidasAlCrear() {
        assertThrows(ReglaDeNegocioException.class, () -> e.borrador(ana, e.oferta().franjasEn(-2L)));
        assertThrows(ValidacionException.class, () -> app.publicacionUseCase.crearBorrador(ana.id(),
                new CrearPublicacionCommand(TipoPublicacion.MENTORIA, "T", "d", "c", Modalidad.REMOTA, 60, 1,
                        BigDecimal.TEN, Set.of(), Set.of(), null, java.util.Collections.singletonList(null))));
    }

    // ---------- editar ----------

    @Test
    @DisplayName("el autor edita su borrador y los cambios quedan guardados")
    void editarBorrador() {
        PublicacionDto borrador = e.borrador(ana, e.oferta());

        PublicacionDto editada = app.publicacionUseCase.editar(ana.id(), borrador.id(), edicion("Titulo editado"));

        assertEquals("Titulo editado", editada.titulo());
        assertEquals("arquitectura", editada.categoria());
        assertEquals(Modalidad.PRESENCIAL, editada.modalidad());
        assertEquals(3, editada.cupo());
        Escenario.assertMonto("55", editada.precioFinal());
        assertEquals(TipoPoliticaCancelacion.ESTRICTA, editada.politicaCancelacion());
        assertEquals("Titulo editado", e.publicacion(ana, borrador).titulo());
    }

    @Test
    @DisplayName("un tercero NO puede editar una publicacion ajena: NoAutorizado y la publicacion no cambia")
    void editarAjenaEsNoAutorizado() {
        PublicacionDto borrador = e.borrador(ana, e.oferta());

        assertThrows(NoAutorizadoException.class, () -> app.publicacionUseCase.editar(bruno.id(), borrador.id(), edicion("Hackeada")));

        assertEquals("Mentoria de Java", e.publicacion(ana, borrador).titulo());
    }

    @Test
    @DisplayName("solo se edita en BORRADOR: una publicacion publicada rechaza la edicion")
    void editarPublicadaFalla() {
        PublicacionDto publicada = e.publicada(ana);

        assertThrows(ReglaDeNegocioException.class, () -> app.publicacionUseCase.editar(ana.id(), publicada.id(), edicion("Nuevo")));
        assertEquals("Mentoria de Java", e.publicacion(ana, publicada).titulo());
    }

    @Test
    @DisplayName("editar una publicacion inexistente es RecursoNoEncontrado")
    void editarInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> app.publicacionUseCase.editar(ana.id(), 999L, edicion("x")));
        assertThrows(ValidacionException.class, () -> app.publicacionUseCase.editar(ana.id(), 1L, null));
    }

    // ---------- franjas ----------

    @Test
    @DisplayName("el autor agrega una franja futura y recibe su id")
    void agregarFranja() {
        PublicacionDto borrador = e.borrador(ana, e.oferta().sinFranjas());
        assertTrue(borrador.franjas().isEmpty());

        PublicacionDto conFranja = app.publicacionUseCase.agregarFranja(ana.id(), borrador.id(),
                app.reloj.ahora().plusDays(5), app.reloj.ahora().plusDays(5).plusHours(1));

        assertEquals(1, conFranja.franjas().size());
        assertNotNull(conFranja.franjas().get(0).id());
        assertFalse(conFranja.franjas().get(0).ocupada());
    }

    @Test
    @DisplayName("agregar franja: pasada o con fin invalido es ReglaDeNegocio, ajena es NoAutorizado, fechas nulas ValidacionException")
    void agregarFranjaInvalida() {
        PublicacionDto borrador = e.borrador(ana, e.oferta());

        assertThrows(ReglaDeNegocioException.class, () -> app.publicacionUseCase.agregarFranja(ana.id(), borrador.id(),
                app.reloj.ahora().minusHours(1), app.reloj.ahora().plusHours(1)));
        assertThrows(ReglaDeNegocioException.class, () -> app.publicacionUseCase.agregarFranja(ana.id(), borrador.id(),
                app.reloj.ahora().plusDays(3), app.reloj.ahora().plusDays(2)));
        assertThrows(NoAutorizadoException.class, () -> app.publicacionUseCase.agregarFranja(bruno.id(), borrador.id(),
                app.reloj.ahora().plusDays(5), app.reloj.ahora().plusDays(5).plusHours(1)));
        assertThrows(ValidacionException.class, () -> app.publicacionUseCase.agregarFranja(ana.id(), borrador.id(), null, null));
        assertEquals(2, e.publicacion(ana, borrador).franjas().size());
    }

    // ---------- transiciones ----------

    @Test
    @DisplayName("publicar -> pausar -> reanudar -> cerrar recorre los estados y deja el historial")
    void cicloDeVida() {
        PublicacionDto borrador = e.borrador(ana, e.oferta());

        PublicacionDto publicada = app.publicacionUseCase.publicar(ana.id(), borrador.id());
        assertEquals("PUBLICADA", publicada.estado());
        assertNotNull(publicada.publicadaEn());
        assertEquals("PAUSADA", app.publicacionUseCase.pausar(ana.id(), borrador.id()).estado());
        assertEquals("PUBLICADA", app.publicacionUseCase.reanudar(ana.id(), borrador.id()).estado());
        PublicacionDto cerrada = app.publicacionUseCase.cerrar(ana.id(), borrador.id());

        assertEquals("CERRADA", cerrada.estado());
        assertEquals(List.of("BORRADOR", "PUBLICADA", "PAUSADA", "PUBLICADA", "CERRADA"),
                cerrada.historial().stream().map(h -> h.estadoNuevo()).toList());
    }

    @Test
    @DisplayName("cancelar exige motivo, queda registrado en el historial y es terminal")
    void cancelarConMotivo() {
        PublicacionDto publicada = e.publicada(ana);

        assertThrows(ReglaDeNegocioException.class, () -> app.publicacionUseCase.cancelar(ana.id(), publicada.id(), " "));
        assertEquals("PUBLICADA", e.publicacion(ana, publicada).estado());

        PublicacionDto cancelada = app.publicacionUseCase.cancelar(ana.id(), publicada.id(), "Ya no puedo dictarla");
        assertEquals("CANCELADA", cancelada.estado());
        assertEquals("Ya no puedo dictarla", cancelada.historial().get(cancelada.historial().size() - 1).motivo());
        assertThrows(TransicionInvalidaException.class, () -> app.publicacionUseCase.reanudar(ana.id(), publicada.id()));
        assertThrows(ReglaDeNegocioException.class, () -> app.publicacionUseCase.agregarFranja(ana.id(), publicada.id(),
                app.reloj.ahora().plusDays(9), app.reloj.ahora().plusDays(9).plusHours(1)));
    }

    @Test
    @DisplayName("las transiciones invalidas lanzan TransicionInvalidaException y no cambian el estado")
    void transicionesInvalidas() {
        PublicacionDto borrador = e.borrador(ana, e.oferta());

        assertThrows(TransicionInvalidaException.class, () -> app.publicacionUseCase.pausar(ana.id(), borrador.id()));
        assertThrows(TransicionInvalidaException.class, () -> app.publicacionUseCase.reanudar(ana.id(), borrador.id()));
        assertThrows(TransicionInvalidaException.class, () -> app.publicacionUseCase.cerrar(ana.id(), borrador.id()));
        app.publicacionUseCase.publicar(ana.id(), borrador.id());
        assertThrows(TransicionInvalidaException.class, () -> app.publicacionUseCase.publicar(ana.id(), borrador.id()));
        assertThrows(TransicionInvalidaException.class, () -> app.publicacionUseCase.reanudar(ana.id(), borrador.id()));
        assertEquals("PUBLICADA", e.publicacion(ana, borrador).estado());
    }

    @Test
    @DisplayName("publicar una publicacion incompleta (sin franjas) falla y sigue en BORRADOR")
    void publicarSinFranjas() {
        PublicacionDto sinFranjas = e.borrador(ana, e.oferta().sinFranjas());

        assertThrows(ReglaDeNegocioException.class, () -> app.publicacionUseCase.publicar(ana.id(), sinFranjas.id()));
        assertEquals("BORRADOR", e.publicacion(ana, sinFranjas).estado());
    }

    @Test
    @DisplayName("un tercero NO puede publicar, pausar, reanudar, cerrar ni cancelar una publicacion ajena")
    void transicionesAjenasSonNoAutorizado() {
        PublicacionDto borrador = e.borrador(ana, e.oferta());
        PublicacionDto publicada = e.publicada(ana);

        assertThrows(NoAutorizadoException.class, () -> app.publicacionUseCase.publicar(bruno.id(), borrador.id()));
        assertThrows(NoAutorizadoException.class, () -> app.publicacionUseCase.pausar(bruno.id(), publicada.id()));
        assertThrows(NoAutorizadoException.class, () -> app.publicacionUseCase.reanudar(bruno.id(), publicada.id()));
        assertThrows(NoAutorizadoException.class, () -> app.publicacionUseCase.cerrar(bruno.id(), publicada.id()));
        assertThrows(NoAutorizadoException.class, () -> app.publicacionUseCase.cancelar(bruno.id(), publicada.id(), "x"));
        assertEquals("BORRADOR", e.publicacion(ana, borrador).estado());
        assertEquals("PUBLICADA", e.publicacion(ana, publicada).estado());
    }

    @Test
    @DisplayName("operar sobre una publicacion inexistente es RecursoNoEncontrado")
    void transicionesInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> app.publicacionUseCase.publicar(ana.id(), 999L));
        assertThrows(RecursoNoEncontradoException.class, () -> app.publicacionUseCase.pausar(ana.id(), 999L));
        assertThrows(RecursoNoEncontradoException.class, () -> app.publicacionUseCase.obtener(ana.id(), 999L));
        assertThrows(ValidacionException.class, () -> app.publicacionUseCase.publicar(null, 1L));
        assertThrows(ValidacionException.class, () -> app.publicacionUseCase.publicar(ana.id(), null));
    }

    // ---------- obtener y listar ----------

    @Test
    @DisplayName("un borrador o una pausada solo los ve su autor; una publicada la ve cualquiera")
    void visibilidad() {
        PublicacionDto borrador = e.borrador(ana, e.oferta());
        PublicacionDto publicada = e.publicada(ana);

        assertEquals(borrador.id(), app.publicacionUseCase.obtener(ana.id(), borrador.id()).id());
        assertThrows(NoAutorizadoException.class, () -> app.publicacionUseCase.obtener(bruno.id(), borrador.id()));
        assertEquals(publicada.id(), app.publicacionUseCase.obtener(bruno.id(), publicada.id()).id());

        app.publicacionUseCase.pausar(ana.id(), publicada.id());
        assertThrows(NoAutorizadoException.class, () -> app.publicacionUseCase.obtener(bruno.id(), publicada.id()));
        assertEquals("PAUSADA", app.publicacionUseCase.obtener(ana.id(), publicada.id()).estado());
    }

    @Test
    @DisplayName("listarPropias pagina solo las publicaciones del autor, de la mas reciente a la mas antigua")
    void listarPropias() {
        e.publicada(ana, e.oferta().titulo("Primera"));
        e.avanzarHoras(1);
        e.publicada(ana, e.oferta().titulo("Segunda"));
        e.avanzarHoras(1);
        e.borrador(ana, e.oferta().titulo("Tercera"));
        e.publicada(bruno, e.oferta().titulo("De Bruno"));

        Pagina<PublicacionResumenDto> pagina1 = app.publicacionUseCase.listarPropias(ana.id(), PaginaRequest.de(0, 2));
        Pagina<PublicacionResumenDto> pagina2 = app.publicacionUseCase.listarPropias(ana.id(), PaginaRequest.de(1, 2));

        assertEquals(3L, pagina1.totalElementos());
        assertEquals(2, pagina1.totalPaginas());
        assertEquals(List.of("Tercera", "Segunda"), pagina1.contenido().stream().map(r -> r.titulo()).toList());
        assertEquals(List.of("Primera"), pagina2.contenido().stream().map(r -> r.titulo()).toList());
        assertTrue(pagina1.contenido().stream().allMatch(r -> r.autorId().equals(ana.id()) && "Ana".equals(r.autorNombre())));
        assertThrows(ValidacionException.class, () -> app.publicacionUseCase.listarPropias(ana.id(), null));
    }

    @Test
    @DisplayName("listarPublicadasDe devuelve solo las PUBLICADAS del autor, de la mas reciente a la mas antigua y hasta el limite")
    void listarPublicadasDe() {
        e.publicada(ana, e.oferta().titulo("Primera"));
        e.avanzarHoras(1);
        PublicacionDto pausada = e.publicada(ana, e.oferta().titulo("Pausada"));
        app.publicacionUseCase.pausar(ana.id(), pausada.id());
        e.avanzarHoras(1);
        e.publicada(ana, e.oferta().titulo("Segunda"));
        e.borrador(ana, e.oferta().titulo("Borrador"));
        e.publicada(bruno, e.oferta().titulo("De Bruno"));

        List<PublicacionResumenDto> todas = app.publicacionUseCase.listarPublicadasDe(ana.id(), 10);
        List<PublicacionResumenDto> una = app.publicacionUseCase.listarPublicadasDe(ana.id(), 1);

        assertEquals(List.of("Segunda", "Primera"), todas.stream().map(PublicacionResumenDto::titulo).toList());
        assertEquals(List.of("Segunda"), una.stream().map(PublicacionResumenDto::titulo).toList());
        assertTrue(app.publicacionUseCase.listarPublicadasDe(ana.id(), 0).isEmpty());
        assertThrows(RecursoNoEncontradoException.class, () -> app.publicacionUseCase.listarPublicadasDe(999L, 5));
    }

    // ---------- promociones ----------

    @Test
    @DisplayName("el autor agrega una promocion vigente y el precio final baja (Decorator); el base no cambia")
    void agregarPromocion() {
        PublicacionDto publicada = e.publicada(ana);

        PublicacionDto conPromo = app.publicacionUseCase.agregarPromocion(ana.id(), new CrearPromocionCommand(
                publicada.id(), "Lanzamiento", TipoPromocion.PORCENTAJE, new BigDecimal("25"),
                app.reloj.ahora().minusDays(1), app.reloj.ahora().plusDays(30)));

        Escenario.assertMonto("40", conPromo.precioBase());
        Escenario.assertMonto("30", conPromo.precioFinal());
        assertEquals(2, conPromo.detallePrecio().size());
    }

    @Test
    @DisplayName("las promociones vigentes se acumulan; la monto fijo mayor al precio deja el total en cero, nunca negativo")
    void promocionesAcumulables() {
        PublicacionDto publicada = e.publicada(ana);

        app.publicacionUseCase.agregarPromocion(ana.id(), new CrearPromocionCommand(publicada.id(), "Mitad de precio",
                TipoPromocion.PORCENTAJE, new BigDecimal("50"), null, null));
        PublicacionDto acumulada = app.publicacionUseCase.agregarPromocion(ana.id(), new CrearPromocionCommand(
                publicada.id(), "Cinco menos", TipoPromocion.MONTO_FIJO, new BigDecimal("5"), null, null));
        Escenario.assertMonto("15", acumulada.precioFinal()); // 40 -50% = 20, -5 = 15

        PublicacionDto enCero = app.publicacionUseCase.agregarPromocion(ana.id(), new CrearPromocionCommand(
                publicada.id(), "Regalo", TipoPromocion.MONTO_FIJO, new BigDecimal("500"), null, null));
        Escenario.assertMonto("0", enCero.precioFinal());
    }

    @Test
    @DisplayName("una promocion vencida o futura no se aplica al precio final")
    void promocionFueraDeVigencia() {
        PublicacionDto publicada = e.publicada(ana);

        app.publicacionUseCase.agregarPromocion(ana.id(), new CrearPromocionCommand(publicada.id(), "Vencida",
                TipoPromocion.PORCENTAJE, new BigDecimal("50"), app.reloj.ahora().minusDays(10), app.reloj.ahora().minusDays(1)));
        PublicacionDto tras = app.publicacionUseCase.agregarPromocion(ana.id(), new CrearPromocionCommand(publicada.id(),
                "Futura", TipoPromocion.PORCENTAJE, new BigDecimal("50"), app.reloj.ahora().plusDays(1), null));

        Escenario.assertMonto("40", tras.precioFinal());
    }

    @Test
    @DisplayName("un tercero NO puede agregar promociones a una publicacion ajena")
    void promocionAjenaEsNoAutorizado() {
        PublicacionDto publicada = e.publicada(ana);

        assertThrows(NoAutorizadoException.class, () -> app.publicacionUseCase.agregarPromocion(bruno.id(),
                new CrearPromocionCommand(publicada.id(), "Truchada", TipoPromocion.PORCENTAJE, new BigDecimal("90"), null, null)));

        Escenario.assertMonto("40", e.publicacion(ana, publicada).precioFinal());
    }

    @Test
    @DisplayName("promocion invalida: sin descripcion, porcentaje mayor a 100, publicacion inexistente o comando nulo")
    void promocionInvalida() {
        PublicacionDto publicada = e.publicada(ana);

        assertThrows(ValidacionException.class, () -> app.publicacionUseCase.agregarPromocion(ana.id(),
                new CrearPromocionCommand(publicada.id(), " ", TipoPromocion.PORCENTAJE, BigDecimal.TEN, null, null)));
        assertThrows(ReglaDeNegocioException.class, () -> app.publicacionUseCase.agregarPromocion(ana.id(),
                new CrearPromocionCommand(publicada.id(), "Mucho", TipoPromocion.PORCENTAJE, new BigDecimal("101"), null, null)));
        assertThrows(RecursoNoEncontradoException.class, () -> app.publicacionUseCase.agregarPromocion(ana.id(),
                new CrearPromocionCommand(999L, "Nada", TipoPromocion.PORCENTAJE, BigDecimal.TEN, null, null)));
        assertThrows(ValidacionException.class, () -> app.publicacionUseCase.agregarPromocion(ana.id(), null));
        Escenario.assertMonto("40", e.publicacion(ana, publicada).precioFinal());
    }

    @Test
    @DisplayName("cada operacion que escribe corre en una sola transaccion")
    void usaTransacciones() {
        int antes = app.transaccion.ejecuciones();

        PublicacionDto borrador = e.borrador(ana, e.oferta());
        assertEquals(antes + 1, app.transaccion.ejecuciones());
        app.publicacionUseCase.publicar(ana.id(), borrador.id());
        assertEquals(antes + 2, app.transaccion.ejecuciones());
    }
}
