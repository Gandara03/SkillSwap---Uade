package com.uade.skillswap.domain.model.publicacion;

import com.uade.skillswap.domain.exception.CupoAgotadoException;
import com.uade.skillswap.domain.exception.FranjaNoDisponibleException;
import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.exception.TransicionInvalidaException;
import com.uade.skillswap.domain.model.historial.HistorialEstado;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.support.Datos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static com.uade.skillswap.support.Datos.AHORA;
import static com.uade.skillswap.support.Datos.CLIENTE;
import static com.uade.skillswap.support.Datos.OTRO;
import static com.uade.skillswap.support.Datos.PROVEEDOR;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Publicacion (agregado + State)")
class PublicacionTest {

    private static Publicacion borradorVacio() {
        return Publicacion.crearBorrador(TipoPublicacion.MENTORIA, PROVEEDOR, TipoPerfil.PROFESIONAL,
                new DatosPublicacion(null, null, null, null, 0, 0, null, null, null, null), AHORA);
    }

    /** Datos con descripcion "d" y sin etiquetas, habilidades ni politica (queda la que tenga la publicacion). */
    private static DatosPublicacion datos(String titulo, String categoria, Modalidad modalidad, int duracionMinutos,
                                          int cupo, SkillCoins precioBase) {
        return new DatosPublicacion(titulo, "d", categoria, modalidad, duracionMinutos, cupo, precioBase,
                null, null, null);
    }

    // ---------- creacion ----------

    @Test
    @DisplayName("nace en BORRADOR con historial inicial y politica FLEXIBLE por defecto")
    void creacion() {
        Publicacion p = Publicacion.crearBorrador(TipoPublicacion.SERVICIO, PROVEEDOR, TipoPerfil.PROFESIONAL,
                datos("t", "c", Modalidad.REMOTA, 30, 2, SkillCoins.de(10)), AHORA);
        assertEquals("BORRADOR", p.getEstado());
        assertEquals(TipoPoliticaCancelacion.FLEXIBLE, p.getPoliticaCancelacion());
        assertEquals(1, p.getHistorial().size());
        HistorialEstado h = p.getHistorial().get(0);
        assertNull(h.getEstadoAnterior());
        assertEquals("BORRADOR", h.getEstadoNuevo());
        assertEquals(PROVEEDOR, h.getActorId());
        assertFalse(p.estaPublicada());
        assertFalse(p.admiteSolicitudes());
        assertEquals(2, p.getCupoDisponible());
    }

    @Test
    @DisplayName("solo una organizacion puede crear un desafio")
    void desafioSoloOrganizacion() {
        DatosPublicacion datos = datos("t", "c", Modalidad.REMOTA, 30, 1, SkillCoins.CERO);
        assertThrows(ReglaDeNegocioException.class, () -> Publicacion.crearBorrador(TipoPublicacion.DESAFIO, PROVEEDOR,
                TipoPerfil.PROFESIONAL, datos, AHORA));
        Publicacion ok = Publicacion.crearBorrador(TipoPublicacion.DESAFIO, 9L, TipoPerfil.ORGANIZACION, datos, AHORA);
        assertEquals(TipoPublicacion.DESAFIO, ok.getTipo());
    }

    @Test
    @DisplayName("una organizacion no puede crear mentorias ni servicios")
    void organizacionNoMentorias() {
        DatosPublicacion datos = datos("t", "c", Modalidad.REMOTA, 30, 1, SkillCoins.CERO);
        assertThrows(ReglaDeNegocioException.class, () -> Publicacion.crearBorrador(TipoPublicacion.MENTORIA, 9L,
                TipoPerfil.ORGANIZACION, datos, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> Publicacion.crearBorrador(TipoPublicacion.SERVICIO, 9L,
                TipoPerfil.ORGANIZACION, datos, AHORA));
    }

    @Test
    @DisplayName("tipo, autor, tipo de autor y datos son obligatorios")
    void obligatorios() {
        DatosPublicacion datos = datos("t", "c", Modalidad.REMOTA, 30, 1, SkillCoins.CERO);
        assertThrows(ReglaDeNegocioException.class, () -> Publicacion.crearBorrador(null, PROVEEDOR,
                TipoPerfil.PROFESIONAL, datos, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> Publicacion.crearBorrador(TipoPublicacion.MENTORIA, null,
                TipoPerfil.PROFESIONAL, datos, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> Publicacion.crearBorrador(TipoPublicacion.MENTORIA, PROVEEDOR,
                null, datos, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> Publicacion.crearBorrador(TipoPublicacion.MENTORIA, PROVEEDOR,
                TipoPerfil.PROFESIONAL, null, AHORA));
    }

    // ---------- edicion ----------

    @Test
    @DisplayName("solo se edita en BORRADOR: los datos cambian")
    void editarBorrador() {
        Publicacion p = Datos.borradorCompleto(PROVEEDOR, 1);
        p.editar(new DatosPublicacion("Nuevo", "Desc", "diseno", Modalidad.PRESENCIAL, 90, 3, SkillCoins.de(50),
                List.of("ux"), List.of(Habilidad.de("Figma")), TipoPoliticaCancelacion.ESTRICTA));
        assertEquals("Nuevo", p.getTitulo());
        assertEquals("diseno", p.getCategoria());
        assertEquals(Modalidad.PRESENCIAL, p.getModalidad());
        assertEquals(90, p.getDuracionMinutos());
        assertEquals(3, p.getCupo());
        assertEquals(SkillCoins.de(50), p.getPrecioBase());
        assertEquals(TipoPoliticaCancelacion.ESTRICTA, p.getPoliticaCancelacion());
        assertTrue(p.getEtiquetas().contains("ux"));
        assertEquals(1, p.getEtiquetas().size());
        assertTrue(p.getHabilidades().contains(Habilidad.de("figma")));
    }

    @Test
    @DisplayName("editar con politica nula conserva la politica actual")
    void editarPoliticaNula() {
        Publicacion p = Datos.borradorCompleto(PROVEEDOR, 1);
        p.editar(datos("t", "c", Modalidad.REMOTA, 60, 1, SkillCoins.de(1)));
        assertEquals(TipoPoliticaCancelacion.FLEXIBLE, p.getPoliticaCancelacion());
        assertTrue(p.getEtiquetas().isEmpty());
    }

    @Test
    @DisplayName("editar exige los datos y no cambia nada si faltan")
    void editarSinDatos() {
        Publicacion p = Datos.borradorCompleto(PROVEEDOR, 1);
        assertThrows(ReglaDeNegocioException.class, () -> p.editar(null));
        assertEquals("Mentoria de Java", p.getTitulo());
    }

    @Test
    @DisplayName("no se puede editar una publicacion PUBLICADA, PAUSADA, CERRADA ni CANCELADA")
    void editarNoBorrador() {
        DatosPublicacion nuevos = datos("x", "c", Modalidad.REMOTA, 60, 1, SkillCoins.de(1));
        Publicacion publicada = Datos.publicada(PROVEEDOR, 1);
        assertThrows(ReglaDeNegocioException.class, () -> publicada.editar(nuevos));
        assertEquals("Mentoria de Java", publicada.getTitulo());

        Publicacion pausada = Datos.publicada(PROVEEDOR, 1);
        pausada.pausar(PROVEEDOR, AHORA);
        assertThrows(ReglaDeNegocioException.class, () -> pausada.editar(nuevos));

        Publicacion cerrada = Datos.publicada(PROVEEDOR, 1);
        cerrada.cerrar(PROVEEDOR, AHORA);
        assertThrows(ReglaDeNegocioException.class, () -> cerrada.editar(nuevos));

        Publicacion cancelada = Datos.borradorCompleto(PROVEEDOR, 1);
        cancelada.cancelar(PROVEEDOR, "ya no", AHORA);
        assertThrows(ReglaDeNegocioException.class, () -> cancelada.editar(nuevos));
    }

    // ---------- franjas ----------

    @Test
    @DisplayName("agregarFranja suma una franja libre futura")
    void agregarFranja() {
        Publicacion p = borradorVacio();
        Franja f = p.agregarFranja(AHORA.plusDays(1), AHORA.plusDays(1).plusHours(1), AHORA);
        assertEquals(1, p.getFranjas().size());
        assertFalse(f.estaOcupada());
    }

    @Test
    @DisplayName("agregarFranja rechaza franjas que ya comenzaron o con fin anterior al inicio")
    void agregarFranjaInvalida() {
        Publicacion p = borradorVacio();
        assertThrows(ReglaDeNegocioException.class, () -> p.agregarFranja(AHORA, AHORA.plusHours(1), AHORA));
        assertThrows(ReglaDeNegocioException.class,
                () -> p.agregarFranja(AHORA.minusDays(1), AHORA.plusHours(1), AHORA));
        assertThrows(ReglaDeNegocioException.class,
                () -> p.agregarFranja(AHORA.plusDays(2), AHORA.plusDays(1), AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> p.agregarFranja(null, AHORA.plusDays(1), AHORA));
        assertTrue(p.getFranjas().isEmpty());
    }

    @Test
    @DisplayName("no se agregan franjas a publicaciones CERRADAS o CANCELADAS")
    void agregarFranjaEnTerminales() {
        Publicacion cerrada = Datos.publicada(PROVEEDOR, 1);
        cerrada.cerrar(PROVEEDOR, AHORA);
        assertThrows(ReglaDeNegocioException.class,
                () -> cerrada.agregarFranja(AHORA.plusDays(5), AHORA.plusDays(5).plusHours(1), AHORA));
        Publicacion cancelada = Datos.borradorCompleto(PROVEEDOR, 1);
        cancelada.cancelar(PROVEEDOR, "x", AHORA);
        assertThrows(ReglaDeNegocioException.class,
                () -> cancelada.agregarFranja(AHORA.plusDays(5), AHORA.plusDays(5).plusHours(1), AHORA));
    }

    @Test
    @DisplayName("se pueden agregar franjas a una publicacion PUBLICADA o PAUSADA")
    void agregarFranjaEnPublicada() {
        Publicacion p = Datos.publicada(PROVEEDOR, 1);
        p.agregarFranja(AHORA.plusDays(7), AHORA.plusDays(7).plusHours(1), AHORA);
        p.pausar(PROVEEDOR, AHORA);
        p.agregarFranja(AHORA.plusDays(8), AHORA.plusDays(8).plusHours(1), AHORA);
        assertEquals(3, p.getFranjas().size());
    }

    @Test
    @DisplayName("buscarFranja devuelve vacio para ids nulos o inexistentes")
    void buscarFranja() {
        Publicacion p = Datos.publicada(PROVEEDOR, 2);
        assertTrue(p.buscarFranja(1L).isPresent());
        assertTrue(p.buscarFranja(99L).isEmpty());
        assertTrue(p.buscarFranja(null).isEmpty());
    }

    // ---------- publicar ----------

    @Test
    @DisplayName("publicar un borrador completo lo deja PUBLICADA, visible, con fecha y registro en el historial")
    void publicarFeliz() {
        Publicacion p = Datos.borradorCompleto(PROVEEDOR, 1);
        LocalDateTime t = AHORA.plusHours(1);
        p.publicar(PROVEEDOR, t);
        assertEquals("PUBLICADA", p.getEstado());
        assertTrue(p.estaPublicada());
        assertTrue(p.admiteSolicitudes());
        assertEquals(t, p.getPublicadaEn());
        assertEquals(2, p.getHistorial().size());
        HistorialEstado h = p.getHistorial().get(1);
        assertEquals("BORRADOR", h.getEstadoAnterior());
        assertEquals("PUBLICADA", h.getEstadoNuevo());
        assertEquals(t, h.getFecha());
    }

    @Test
    @DisplayName("publicar una publicacion incompleta falla y no cambia de estado")
    void publicarIncompleta() {
        Publicacion p = borradorVacio();
        ReglaDeNegocioException e = assertThrows(ReglaDeNegocioException.class, () -> p.publicar(PROVEEDOR, AHORA));
        assertTrue(e.getMessage().contains("titulo"));
        assertTrue(e.getMessage().contains("categoria"));
        assertTrue(e.getMessage().contains("franja"));
        assertEquals("BORRADOR", p.getEstado());
        assertNull(p.getPublicadaEn());
        assertEquals(1, p.getHistorial().size());
    }

    @Test
    @DisplayName("cada dato faltante impide publicar: titulo, categoria, precio, franja, cupo, duracion, modalidad")
    void publicarFaltaCadaDato() {
        assertIncompleta((p) -> p.editar(datos(null, "c", Modalidad.REMOTA, 60, 1, SkillCoins.de(1))));
        assertIncompleta((p) -> p.editar(datos("  ", "c", Modalidad.REMOTA, 60, 1, SkillCoins.de(1))));
        assertIncompleta((p) -> p.editar(datos("t", null, Modalidad.REMOTA, 60, 1, SkillCoins.de(1))));
        assertIncompleta((p) -> p.editar(datos("t", "c", Modalidad.REMOTA, 60, 1, null)));
        assertIncompleta((p) -> p.editar(datos("t", "c", Modalidad.REMOTA, 60, 1, SkillCoins.de(-1))));
        assertIncompleta((p) -> p.editar(datos("t", "c", Modalidad.REMOTA, 60, 0, SkillCoins.de(1))));
        assertIncompleta((p) -> p.editar(datos("t", "c", Modalidad.REMOTA, 0, 1, SkillCoins.de(1))));
        assertIncompleta((p) -> p.editar(datos("t", "c", null, 60, 1, SkillCoins.de(1))));
    }

    private interface Edicion {
        void aplicar(Publicacion p);
    }

    private static void assertIncompleta(Edicion edicion) {
        Publicacion p = Datos.borradorCompleto(PROVEEDOR, 1);
        edicion.aplicar(p);
        assertThrows(ReglaDeNegocioException.class, () -> p.publicar(PROVEEDOR, AHORA));
        assertEquals("BORRADOR", p.getEstado());
    }

    @Test
    @DisplayName("un precio base en cero es valido para publicar (gratuita)")
    void publicarGratuita() {
        Publicacion p = Datos.borradorCompleto(PROVEEDOR, 1, 1, SkillCoins.CERO);
        assertDoesNotThrow(() -> p.publicar(PROVEEDOR, AHORA));
    }

    @Test
    @DisplayName("sin franjas no se puede publicar")
    void publicarSinFranjas() {
        Publicacion p = Datos.borradorCompleto(PROVEEDOR, 0);
        assertThrows(ReglaDeNegocioException.class, () -> p.publicar(PROVEEDOR, AHORA));
    }

    @Test
    @DisplayName("publicar exige un actor")
    void publicarSinActor() {
        Publicacion p = Datos.borradorCompleto(PROVEEDOR, 1);
        assertThrows(ReglaDeNegocioException.class, () -> p.publicar(null, AHORA));
        assertEquals("BORRADOR", p.getEstado());
    }

    @Test
    @DisplayName("la fecha de primera publicacion se conserva al reanudar")
    void publicadaEnSeConserva() {
        Publicacion p = Datos.borradorCompleto(PROVEEDOR, 1);
        p.publicar(PROVEEDOR, AHORA);
        p.pausar(PROVEEDOR, AHORA.plusDays(1));
        p.reanudar(PROVEEDOR, AHORA.plusDays(2));
        assertEquals(AHORA, p.getPublicadaEn());
    }

    // ---------- transiciones validas ----------

    @Test
    @DisplayName("PUBLICADA -> PAUSADA -> PUBLICADA -> CERRADA registra cada transicion con actor y fecha")
    void cicloPublicadaPausadaCerrada() {
        Publicacion p = Datos.publicada(PROVEEDOR, 1);
        p.pausar(PROVEEDOR, AHORA.plusHours(1));
        assertEquals("PAUSADA", p.getEstado());
        assertFalse(p.admiteSolicitudes());
        p.reanudar(PROVEEDOR, AHORA.plusHours(2));
        assertEquals("PUBLICADA", p.getEstado());
        assertTrue(p.admiteSolicitudes());
        p.cerrar(PROVEEDOR, AHORA.plusHours(3));
        assertEquals("CERRADA", p.getEstado());
        assertEquals(5, p.getHistorial().size());
        assertEquals("PAUSADA", p.getHistorial().get(3).getEstadoAnterior());
        assertEquals("PUBLICADA", p.getHistorial().get(3).getEstadoNuevo());
        assertEquals(AHORA.plusHours(3), p.getHistorial().get(4).getFecha());
        assertEquals(PROVEEDOR, p.getHistorial().get(4).getActorId());
    }

    @Test
    @DisplayName("PAUSADA puede cerrarse directamente")
    void pausadaCerrar() {
        Publicacion p = Datos.publicada(PROVEEDOR, 1);
        p.pausar(PROVEEDOR, AHORA);
        p.cerrar(PROVEEDOR, AHORA);
        assertEquals("CERRADA", p.getEstado());
    }

    @Test
    @DisplayName("cancelar desde BORRADOR, PUBLICADA y PAUSADA con motivo registra el motivo")
    void cancelarDesdeEstadosValidos() {
        Publicacion b = Datos.borradorCompleto(PROVEEDOR, 1);
        b.cancelar(PROVEEDOR, "cambie de idea", AHORA);
        assertEquals("CANCELADA", b.getEstado());
        assertEquals("cambie de idea", b.getHistorial().get(1).getMotivo());

        Publicacion pub = Datos.publicada(PROVEEDOR, 1);
        pub.cancelar(PROVEEDOR, "sin tiempo", AHORA);
        assertEquals("CANCELADA", pub.getEstado());

        Publicacion pau = Datos.publicada(PROVEEDOR, 1);
        pau.pausar(PROVEEDOR, AHORA);
        pau.cancelar(PROVEEDOR, "viaje", AHORA);
        assertEquals("CANCELADA", pau.getEstado());
    }

    @Test
    @DisplayName("cancelar exige motivo (nulo o en blanco falla y no cambia el estado)")
    void cancelarSinMotivo() {
        Publicacion p = Datos.publicada(PROVEEDOR, 1);
        assertThrows(ReglaDeNegocioException.class, () -> p.cancelar(PROVEEDOR, null, AHORA));
        assertThrows(ReglaDeNegocioException.class, () -> p.cancelar(PROVEEDOR, "   ", AHORA));
        assertEquals("PUBLICADA", p.getEstado());
        Publicacion b = Datos.borradorCompleto(PROVEEDOR, 1);
        assertThrows(ReglaDeNegocioException.class, () -> b.cancelar(PROVEEDOR, "", AHORA));
        assertEquals("BORRADOR", b.getEstado());
    }

    // ---------- transiciones invalidas ----------

    @Test
    @DisplayName("BORRADOR no puede pausarse, reanudarse ni cerrarse")
    void invalidasDesdeBorrador() {
        Publicacion p = Datos.borradorCompleto(PROVEEDOR, 1);
        assertThrows(TransicionInvalidaException.class, () -> p.pausar(PROVEEDOR, AHORA));
        assertThrows(TransicionInvalidaException.class, () -> p.reanudar(PROVEEDOR, AHORA));
        assertThrows(TransicionInvalidaException.class, () -> p.cerrar(PROVEEDOR, AHORA));
        assertEquals("BORRADOR", p.getEstado());
    }

    @Test
    @DisplayName("PUBLICADA no puede publicarse de nuevo ni reanudarse")
    void invalidasDesdePublicada() {
        Publicacion p = Datos.publicada(PROVEEDOR, 1);
        assertThrows(TransicionInvalidaException.class, () -> p.publicar(PROVEEDOR, AHORA));
        assertThrows(TransicionInvalidaException.class, () -> p.reanudar(PROVEEDOR, AHORA));
        assertEquals("PUBLICADA", p.getEstado());
    }

    @Test
    @DisplayName("PAUSADA no puede publicarse ni pausarse de nuevo")
    void invalidasDesdePausada() {
        Publicacion p = Datos.publicada(PROVEEDOR, 1);
        p.pausar(PROVEEDOR, AHORA);
        assertThrows(TransicionInvalidaException.class, () -> p.publicar(PROVEEDOR, AHORA));
        assertThrows(TransicionInvalidaException.class, () -> p.pausar(PROVEEDOR, AHORA));
    }

    @Test
    @DisplayName("CERRADA y CANCELADA son terminales: ninguna accion es valida")
    void terminalesNoAdmitenNada() {
        Publicacion cerrada = Datos.publicada(PROVEEDOR, 1);
        cerrada.cerrar(PROVEEDOR, AHORA);
        Publicacion cancelada = Datos.publicada(PROVEEDOR, 1);
        cancelada.cancelar(PROVEEDOR, "x", AHORA);
        for (Publicacion p : List.of(cerrada, cancelada)) {
            assertThrows(TransicionInvalidaException.class, () -> p.publicar(PROVEEDOR, AHORA));
            assertThrows(TransicionInvalidaException.class, () -> p.pausar(PROVEEDOR, AHORA));
            assertThrows(TransicionInvalidaException.class, () -> p.reanudar(PROVEEDOR, AHORA));
            assertThrows(TransicionInvalidaException.class, () -> p.cerrar(PROVEEDOR, AHORA));
            assertThrows(TransicionInvalidaException.class, () -> p.cancelar(PROVEEDOR, "otra vez", AHORA));
        }
        assertEquals("CERRADA", cerrada.getEstado());
        assertEquals("CANCELADA", cancelada.getEstado());
    }

    @Test
    @DisplayName("la transicion invalida informa estado actual y accion")
    void excepcionInformativa() {
        Publicacion p = Datos.borradorCompleto(PROVEEDOR, 1);
        TransicionInvalidaException e = assertThrows(TransicionInvalidaException.class, () -> p.cerrar(PROVEEDOR, AHORA));
        assertEquals("BORRADOR", e.getEstadoActual());
        assertEquals("cerrar", e.getAccion());
    }

    // ---------- visibilidad ----------

    @Test
    @DisplayName("solo PUBLICADA es visible para todos; el autor ve siempre su publicacion")
    void visibilidad() {
        Publicacion borrador = Datos.borradorCompleto(PROVEEDOR, 1);
        assertFalse(borrador.esVisiblePara(CLIENTE));
        assertTrue(borrador.esVisiblePara(PROVEEDOR));
        Publicacion publicada = Datos.publicada(PROVEEDOR, 1);
        assertTrue(publicada.esVisiblePara(CLIENTE));
        publicada.pausar(PROVEEDOR, AHORA);
        assertFalse(publicada.esVisiblePara(CLIENTE));
        assertTrue(publicada.esVisiblePara(PROVEEDOR));
        assertTrue(publicada.esAutor(PROVEEDOR));
        assertFalse(publicada.esAutor(CLIENTE));
    }

    // ---------- validarSolicitud ----------

    @Test
    @DisplayName("un cliente puede solicitar una franja libre futura de una publicacion PUBLICADA")
    void validarSolicitudFeliz() {
        Publicacion p = Datos.publicada(PROVEEDOR, 2);
        assertDoesNotThrow(() -> p.validarSolicitud(CLIENTE, 1L, AHORA));
    }

    @Test
    @DisplayName("no se admiten solicitudes si no esta PUBLICADA")
    void validarSolicitudEstado() {
        Publicacion borrador = Datos.borradorCompleto(PROVEEDOR, 1);
        assertThrows(ReglaDeNegocioException.class, () -> borrador.validarSolicitud(CLIENTE, 1L, AHORA));
        Publicacion pausada = Datos.publicada(PROVEEDOR, 1);
        pausada.pausar(PROVEEDOR, AHORA);
        assertThrows(ReglaDeNegocioException.class, () -> pausada.validarSolicitud(CLIENTE, 1L, AHORA));
        Publicacion cerrada = Datos.publicada(PROVEEDOR, 1);
        cerrada.cerrar(PROVEEDOR, AHORA);
        assertThrows(ReglaDeNegocioException.class, () -> cerrada.validarSolicitud(CLIENTE, 1L, AHORA));
    }

    @Test
    @DisplayName("el autor no puede solicitar su propia publicacion")
    void validarSolicitudAutor() {
        Publicacion p = Datos.publicada(PROVEEDOR, 1);
        assertThrows(ReglaDeNegocioException.class, () -> p.validarSolicitud(PROVEEDOR, 1L, AHORA));
    }

    @Test
    @DisplayName("franja inexistente, ya comenzada u ocupada -> FranjaNoDisponibleException")
    void validarSolicitudFranja() {
        Publicacion p = Datos.publicada(PROVEEDOR, 2, 2, SkillCoins.de(10));
        assertThrows(FranjaNoDisponibleException.class, () -> p.validarSolicitud(CLIENTE, 99L, AHORA));
        assertThrows(FranjaNoDisponibleException.class, () -> p.validarSolicitud(CLIENTE, null, AHORA));
        // franja 1 comienza AHORA + 1 dia: una vez pasado ese instante ya no sirve
        assertThrows(FranjaNoDisponibleException.class,
                () -> p.validarSolicitud(CLIENTE, 1L, AHORA.plusDays(1)));
        assertThrows(FranjaNoDisponibleException.class,
                () -> p.validarSolicitud(CLIENTE, 1L, AHORA.plusDays(3)));
        p.reservarFranja(1L, AHORA);
        assertThrows(FranjaNoDisponibleException.class, () -> p.validarSolicitud(OTRO, 1L, AHORA));
    }

    @Test
    @DisplayName("sin cupo disponible -> CupoAgotadoException")
    void validarSolicitudCupo() {
        Publicacion p = Datos.publicada(PROVEEDOR, 2, 1, SkillCoins.de(10));
        p.reservarFranja(1L, AHORA);
        CupoAgotadoException e = assertThrows(CupoAgotadoException.class, () -> p.validarSolicitud(OTRO, 2L, AHORA));
        assertNotNull(e.getMessage());
    }

    // ---------- reserva ----------

    @Test
    @DisplayName("reservarFranja ocupa la franja y consume un cupo")
    void reservar() {
        Publicacion p = Datos.publicada(PROVEEDOR, 2, 3, SkillCoins.de(10));
        p.reservarFranja(1L, AHORA);
        assertTrue(p.buscarFranja(1L).orElseThrow().estaOcupada());
        assertFalse(p.buscarFranja(2L).orElseThrow().estaOcupada());
        assertEquals(1, p.getCupoOcupado());
        assertEquals(2, p.getCupoDisponible());
    }

    @Test
    @DisplayName("no se puede reservar dos veces la misma franja")
    void dobleReserva() {
        Publicacion p = Datos.publicada(PROVEEDOR, 1, 5, SkillCoins.de(10));
        p.reservarFranja(1L, AHORA);
        assertThrows(FranjaNoDisponibleException.class, () -> p.reservarFranja(1L, AHORA));
        assertEquals(1, p.getCupoOcupado());
    }

    @Test
    @DisplayName("al agotarse el cupo no se reservan mas franjas aunque haya libres")
    void cupoAgotado() {
        Publicacion p = Datos.publicada(PROVEEDOR, 3, 2, SkillCoins.de(10));
        p.reservarFranja(1L, AHORA);
        p.reservarFranja(2L, AHORA);
        assertThrows(CupoAgotadoException.class, () -> p.reservarFranja(3L, AHORA));
        assertFalse(p.buscarFranja(3L).orElseThrow().estaOcupada());
        assertEquals(0, p.getCupoDisponible());
    }

    @Test
    @DisplayName("no se reserva en una franja que ya paso")
    void reservarFranjaPasada() {
        Publicacion p = Datos.publicada(PROVEEDOR, 1, 1, SkillCoins.de(10));
        assertThrows(FranjaNoDisponibleException.class, () -> p.reservarFranja(1L, AHORA.plusDays(2)));
        assertEquals(0, p.getCupoOcupado());
    }

    @Test
    @DisplayName("no se reserva en publicaciones CERRADAS o CANCELADAS")
    void reservarEnTerminal() {
        Publicacion cerrada = Datos.publicada(PROVEEDOR, 1);
        cerrada.cerrar(PROVEEDOR, AHORA);
        assertThrows(ReglaDeNegocioException.class, () -> cerrada.reservarFranja(1L, AHORA));
        Publicacion cancelada = Datos.publicada(PROVEEDOR, 1);
        cancelada.cancelar(PROVEEDOR, "x", AHORA);
        assertThrows(ReglaDeNegocioException.class, () -> cancelada.reservarFranja(1L, AHORA));
    }

    @Test
    @DisplayName("liberarFranja devuelve el cupo y deja la franja libre")
    void liberar() {
        Publicacion p = Datos.publicada(PROVEEDOR, 1, 1, SkillCoins.de(10));
        p.reservarFranja(1L, AHORA);
        p.liberarFranja(1L);
        assertFalse(p.buscarFranja(1L).orElseThrow().estaOcupada());
        assertEquals(0, p.getCupoOcupado());
        assertDoesNotThrow(() -> p.validarSolicitud(CLIENTE, 1L, AHORA));
    }

    @Test
    @DisplayName("liberar una franja libre no resta cupo (nunca queda negativo)")
    void liberarLibre() {
        Publicacion p = Datos.publicada(PROVEEDOR, 2, 2, SkillCoins.de(10));
        p.reservarFranja(1L, AHORA);
        p.liberarFranja(2L);
        assertEquals(1, p.getCupoOcupado());
    }

    @Test
    @DisplayName("liberar una franja inexistente falla")
    void liberarInexistente() {
        Publicacion p = Datos.publicada(PROVEEDOR, 1);
        assertThrows(FranjaNoDisponibleException.class, () -> p.liberarFranja(42L));
    }

    @Test
    @DisplayName("reprogramarReserva mueve la reserva sin alterar el cupo ocupado")
    void reprogramar() {
        Publicacion p = Datos.publicada(PROVEEDOR, 2, 1, SkillCoins.de(10));
        p.reservarFranja(1L, AHORA);
        p.reprogramarReserva(1L, 2L, AHORA);
        assertFalse(p.buscarFranja(1L).orElseThrow().estaOcupada());
        assertTrue(p.buscarFranja(2L).orElseThrow().estaOcupada());
        assertEquals(1, p.getCupoOcupado());
    }

    @Test
    @DisplayName("reprogramar a una franja ocupada o inexistente falla y deja todo como estaba")
    void reprogramarInvalida() {
        Publicacion p = Datos.publicada(PROVEEDOR, 2, 2, SkillCoins.de(10));
        p.reservarFranja(1L, AHORA);
        p.reservarFranja(2L, AHORA);
        assertThrows(FranjaNoDisponibleException.class, () -> p.reprogramarReserva(1L, 2L, AHORA));
        assertThrows(FranjaNoDisponibleException.class, () -> p.reprogramarReserva(1L, 77L, AHORA));
        assertTrue(p.buscarFranja(1L).orElseThrow().estaOcupada());
        assertEquals(2, p.getCupoOcupado());
    }

    @Test
    @DisplayName("las colecciones de la publicacion son de solo lectura")
    void inmutables() {
        Publicacion p = Datos.publicada(PROVEEDOR, 1);
        assertThrows(UnsupportedOperationException.class, () -> p.getFranjas().clear());
        assertThrows(UnsupportedOperationException.class, () -> p.getHistorial().clear());
        assertThrows(UnsupportedOperationException.class, () -> p.getEtiquetas().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> p.getHabilidades().clear());
    }

    @Test
    @DisplayName("reconstruir restaura el estado desde su nombre y mantiene el comportamiento del State")
    void reconstruir() {
        Publicacion original = Datos.publicada(PROVEEDOR, 2, 2, SkillCoins.de(10));
        original.reservarFranja(1L, AHORA);
        Publicacion r = Publicacion.reconstruir(original.getId(), original.getTipo(), original.getAutorId(),
                original.getTitulo(), original.getDescripcion(), original.getCategoria(), original.getModalidad(),
                original.getDuracionMinutos(), original.getCupo(), original.getCupoOcupado(), original.getPrecioBase(),
                original.getPoliticaCancelacion(), original.getEtiquetas(), original.getHabilidades(),
                original.getFranjas(), "PAUSADA", original.getHistorial(), original.getCreadaEn(),
                original.getPublicadaEn(), 4L);
        assertEquals("PAUSADA", r.getEstado());
        assertFalse(r.admiteSolicitudes());
        assertEquals(1, r.getCupoOcupado());
        r.reanudar(PROVEEDOR, AHORA);
        assertEquals("PUBLICADA", r.getEstado());
        assertThrows(IllegalArgumentException.class, () -> Publicacion.reconstruir(1L, TipoPublicacion.MENTORIA,
                PROVEEDOR, "t", "d", "c", Modalidad.REMOTA, 60, 1, 0, SkillCoins.CERO,
                TipoPoliticaCancelacion.FLEXIBLE, List.of(), List.of(), List.of(), "DESCONOCIDO", List.of(), AHORA,
                null, null));
    }
}
