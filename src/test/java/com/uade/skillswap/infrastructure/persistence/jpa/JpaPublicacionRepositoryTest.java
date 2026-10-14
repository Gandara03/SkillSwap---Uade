package com.uade.skillswap.infrastructure.persistence.jpa;

import com.uade.skillswap.application.exception.ConflictoConcurrenciaException;
import com.uade.skillswap.domain.model.historial.HistorialEstado;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.DatosPublicacion;
import com.uade.skillswap.domain.model.publicacion.Franja;
import com.uade.skillswap.domain.model.publicacion.Modalidad;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.publicacion.TipoPublicacion;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaPublicacionRepositoryTest extends JpaTestBase {

    private Perfil ana;

    @BeforeEach
    void crearAutora() {
        ana = profesional("u-ana");
    }

    @Test
    @DisplayName("un borrador se guarda y se recupera completo: datos, politica, colecciones, estado e historial")
    void guardarBorradorYRecuperar() {
        Publicacion borrador = publicacion(ana).borrador().titulo("Mentoria de Java").categoria("programacion")
                .etiquetas("backend", "junior").habilidades("Java", "SQL").precio(150).franjasEn().construir();
        Publicacion guardada = publicaciones.guardar(borrador);

        Publicacion leida = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        assertEquals(TipoPublicacion.MENTORIA, leida.getTipo());
        assertEquals(ana.getId(), leida.getAutorId());
        assertEquals("Mentoria de Java", leida.getTitulo());
        assertEquals("Aprende Java con ejemplos", leida.getDescripcion());
        assertEquals("programacion", leida.getCategoria());
        assertEquals(Modalidad.REMOTA, leida.getModalidad());
        assertEquals(60, leida.getDuracionMinutos());
        assertEquals(5, leida.getCupo());
        assertEquals(0, leida.getCupoOcupado());
        assertEquals(SkillCoins.de(150), leida.getPrecioBase());
        assertEquals(TipoPoliticaCancelacion.FLEXIBLE, leida.getPoliticaCancelacion());
        assertEquals(Set.of("backend", "junior"), leida.getEtiquetas());
        assertEquals(Set.of(Habilidad.de("java"), Habilidad.de("sql")), leida.getHabilidades());
        assertEquals("BORRADOR", leida.getEstado());
        assertEquals(AHORA, leida.getCreadaEn());
        assertNull(leida.getPublicadaEn());
        assertEquals(Long.valueOf(0), leida.getVersion());
        assertEquals(1, leida.getHistorial().size());
        HistorialEstado creacion = leida.getHistorial().get(0);
        assertNull(creacion.getEstadoAnterior());
        assertEquals("BORRADOR", creacion.getEstadoNuevo());
        assertEquals(ana.getId(), creacion.getActorId());
        assertEquals("Borrador creado", creacion.getMotivo());
        assertNotNull(creacion.getId());
    }

    @Test
    @DisplayName("un borrador incompleto (sin titulo, categoria, modalidad ni precio) tambien se persiste")
    void borradorIncompleto() {
        Publicacion incompleta = Publicacion.crearBorrador(TipoPublicacion.SERVICIO, ana.getId(), ana.getTipo(),
                new DatosPublicacion(null, null, null, null, 0, 0, null, null, null, null), AHORA);

        Publicacion leida = publicaciones.buscarPorId(publicaciones.guardar(incompleta).getId()).orElseThrow();

        assertNull(leida.getTitulo());
        assertNull(leida.getModalidad());
        assertNull(leida.getPrecioBase());
        assertEquals("BORRADOR", leida.getEstado());
    }

    @Test
    @DisplayName("las franjas se guardan con id (tambien en la instancia recibida), en orden y sin ocupar")
    void franjasConId() {
        LocalDateTime d1 = AHORA.plusDays(1);
        LocalDateTime d2 = AHORA.plusDays(2);
        LocalDateTime d3 = AHORA.plusDays(3);
        Publicacion guardada = publicacion(ana).franjasEn(d1, d2, d3).guardar();

        for (Franja f : guardada.getFranjas()) {
            assertNotNull(f.getId());
        }
        List<Franja> leidas = publicaciones.buscarPorId(guardada.getId()).orElseThrow().getFranjas();
        assertEquals(3, leidas.size());
        assertIterableEquals(List.of(d1, d2, d3), leidas.stream().map(Franja::getInicio).toList());
        assertEquals(d1.plusHours(1), leidas.get(0).getFin());
        assertIterableEquals(guardada.getFranjas().stream().map(Franja::getId).toList(),
                leidas.stream().map(Franja::getId).toList());
        assertFalse(leidas.get(0).estaOcupada());
    }

    @Test
    @DisplayName("agregar una franja a una publicacion ya guardada inserta solo la nueva")
    void agregarFranjaDespues() {
        Publicacion guardada = publicacion(ana).borrador().franjasEn(AHORA.plusDays(1)).guardar();

        Publicacion editable = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        editable.agregarFranja(AHORA.plusDays(2), AHORA.plusDays(2).plusHours(1), AHORA);
        publicaciones.guardar(editable);

        List<Franja> leidas = publicaciones.buscarPorId(guardada.getId()).orElseThrow().getFranjas();
        assertEquals(2, leidas.size());
        assertEquals(guardada.getFranjas().get(0).getId(), leidas.get(0).getId());
        assertNotNull(editable.getFranjas().get(1).getId());
    }

    @Test
    @DisplayName("las transiciones de estado se persisten con su historial y el estado se reconstruye operativo")
    void transicionesPersistenEstadoEHistorial() {
        Publicacion guardada = publicacion(ana).publicadaEn(AHORA.plusMinutes(5)).guardar();

        Publicacion leida = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        assertEquals("PUBLICADA", leida.getEstado());
        assertTrue(leida.estaPublicada());
        assertEquals(AHORA.plusMinutes(5), leida.getPublicadaEn());
        assertEquals(2, leida.getHistorial().size());
        assertEquals("BORRADOR", leida.getHistorial().get(1).getEstadoAnterior());
        assertEquals("PUBLICADA", leida.getHistorial().get(1).getEstadoNuevo());

        // el State rehidratado sigue funcionando: se puede pausar y reanudar tras volver de la base
        leida.pausar(ana.getId(), AHORA.plusMinutes(10));
        publicaciones.guardar(leida);
        Publicacion pausada = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        assertEquals("PAUSADA", pausada.getEstado());
        assertFalse(pausada.admiteSolicitudes());
        pausada.reanudar(ana.getId(), AHORA.plusMinutes(20));
        publicaciones.guardar(pausada);

        Publicacion reanudada = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        assertEquals("PUBLICADA", reanudada.getEstado());
        assertEquals(4, reanudada.getHistorial().size());
        assertIterableEquals(List.of("BORRADOR", "PUBLICADA", "PAUSADA", "PUBLICADA"),
                reanudada.getHistorial().stream().map(HistorialEstado::getEstadoNuevo).toList());
        assertEquals(Long.valueOf(2), reanudada.getVersion());
    }

    @Test
    @DisplayName("guardar dos veces la misma instancia no duplica franjas ni historial")
    void guardarDosVecesNoDuplica() {
        Publicacion p = publicacion(ana).franjasEn(AHORA.plusDays(1), AHORA.plusDays(2)).construir();

        publicaciones.guardar(p);
        publicaciones.guardar(p);

        Publicacion leida = publicaciones.buscarPorId(p.getId()).orElseThrow();
        assertEquals(2, leida.getFranjas().size());
        assertEquals(2, leida.getHistorial().size());
        assertEquals(Long.valueOf(0), leida.getVersion());
    }

    @Test
    @DisplayName("editar un borrador reemplaza etiquetas y habilidades sincronizando las diferencias")
    void editarBorradorSincronizaColecciones() {
        Publicacion guardada = publicacion(ana).borrador().etiquetas("a", "b").habilidades("java").guardar();

        Publicacion editable = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        editable.editar(new DatosPublicacion("Nuevo titulo", "Nueva descripcion", "backend", Modalidad.PRESENCIAL,
                90, 3, SkillCoins.de(250), List.of("b", "c"), List.of(Habilidad.de("python")),
                TipoPoliticaCancelacion.ESTRICTA));
        publicaciones.guardar(editable);

        Publicacion leida = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        assertEquals("Nuevo titulo", leida.getTitulo());
        assertEquals(Modalidad.PRESENCIAL, leida.getModalidad());
        assertEquals(90, leida.getDuracionMinutos());
        assertEquals(SkillCoins.de(250), leida.getPrecioBase());
        assertEquals(TipoPoliticaCancelacion.ESTRICTA, leida.getPoliticaCancelacion());
        assertEquals(Set.of("b", "c"), leida.getEtiquetas());
        assertEquals(Set.of(Habilidad.de("python")), leida.getHabilidades());
    }

    @Test
    @DisplayName("reservar una franja persiste el cupo ocupado y la franja ocupada")
    void reservarFranja() {
        Publicacion guardada = publicacion(ana).franjasEn(AHORA.plusDays(1), AHORA.plusDays(2)).guardar();
        Long franja = guardada.getFranjas().get(0).getId();

        Publicacion editable = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        editable.reservarFranja(franja, AHORA);
        publicaciones.guardar(editable);

        Publicacion leida = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        assertEquals(1, leida.getCupoOcupado());
        assertTrue(leida.buscarFranja(franja).orElseThrow().estaOcupada());
        assertFalse(leida.getFranjas().get(1).estaOcupada());
        assertEquals(Long.valueOf(1), leida.getVersion());
    }

    @Test
    @DisplayName("reprogramar una reserva (solo cambian franjas) tambien incrementa la version de la publicacion")
    void reprogramarReservaIncrementaLaVersion() {
        Publicacion guardada = publicacion(ana).franjasEn(AHORA.plusDays(1), AHORA.plusDays(2)).guardar();
        Long a = guardada.getFranjas().get(0).getId();
        Long b = guardada.getFranjas().get(1).getId();
        Publicacion reservada = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        reservada.reservarFranja(a, AHORA);
        publicaciones.guardar(reservada);
        Long versionAntes = publicaciones.buscarPorId(guardada.getId()).orElseThrow().getVersion();

        Publicacion editable = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        editable.reprogramarReserva(a, b, AHORA);
        publicaciones.guardar(editable);

        Publicacion leida = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        assertFalse(leida.buscarFranja(a).orElseThrow().estaOcupada());
        assertTrue(leida.buscarFranja(b).orElseThrow().estaOcupada());
        assertEquals(1, leida.getCupoOcupado());
        assertEquals(Long.valueOf(versionAntes + 1), leida.getVersion());
    }

    @Test
    @DisplayName("dos lectores reservan el ultimo cupo: el segundo guardado falla con ConflictoConcurrenciaException")
    void ultimoCupoNoSeSobrevende() {
        Publicacion guardada = publicacion(ana).cupo(1).franjasEn(AHORA.plusDays(1), AHORA.plusDays(2)).guardar();
        Long franja1 = guardada.getFranjas().get(0).getId();
        Long franja2 = guardada.getFranjas().get(1).getId();

        Publicacion lectorA = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        Publicacion lectorB = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        lectorA.reservarFranja(franja1, AHORA);
        lectorB.reservarFranja(franja2, AHORA);

        publicaciones.guardar(lectorA);
        assertThrows(ConflictoConcurrenciaException.class, () -> publicaciones.guardar(lectorB));

        Publicacion leida = publicaciones.buscarPorId(guardada.getId()).orElseThrow();
        assertEquals(1, leida.getCupoOcupado());
        assertTrue(leida.buscarFranja(franja1).orElseThrow().estaOcupada());
        assertFalse(leida.buscarFranja(franja2).orElseThrow().estaOcupada());
    }

    @Test
    @DisplayName("buscarPorId devuelve vacio si no existe o si el id es null")
    void buscarPorIdInexistente() {
        assertTrue(publicaciones.buscarPorId(9999L).isEmpty());
        assertTrue(publicaciones.buscarPorId(null).isEmpty());
    }

    @Test
    @DisplayName("buscarPorAutor incluye todos los estados, las mas recientes primero, paginado")
    void buscarPorAutor() {
        Perfil bruno = profesional("u-bruno");
        Publicacion p1 = publicacion(ana).titulo("P1").publicadaEn(AHORA.plusMinutes(1)).guardar();
        Publicacion p2 = publicacion(ana).titulo("P2").publicadaEn(AHORA.plusMinutes(2)).guardar();
        Publicacion borrador = publicacion(ana).titulo("Borrador").borrador().guardar();
        Publicacion p4 = publicacion(ana).titulo("P4").publicadaEn(AHORA.plusMinutes(4)).guardar();
        publicacion(bruno).titulo("De Bruno").guardar();

        Pagina<Publicacion> primera = publicaciones.buscarPorAutor(ana.getId(), PaginaRequest.de(0, 3));
        Pagina<Publicacion> segunda = publicaciones.buscarPorAutor(ana.getId(), PaginaRequest.de(1, 3));

        assertEquals(4, primera.totalElementos());
        assertEquals(2, primera.totalPaginas());
        assertIterableEquals(List.of(p4.getId(), p2.getId(), p1.getId()),
                primera.contenido().stream().map(Publicacion::getId).toList());
        assertIterableEquals(List.of(borrador.getId()), segunda.contenido().stream().map(Publicacion::getId).toList());
        assertTrue(publicaciones.buscarPorAutor(9999L, PaginaRequest.de(0, 5)).contenido().isEmpty());
    }

    @Test
    @DisplayName("buscarPublicadas devuelve solo las PUBLICADAS, las mas recientes primero y acotadas por el limite")
    void buscarPublicadas() {
        Publicacion p1 = publicacion(ana).publicadaEn(AHORA.plusMinutes(1)).guardar();
        Publicacion p2 = publicacion(ana).publicadaEn(AHORA.plusMinutes(2)).guardar();
        publicacion(ana).borrador().guardar();
        Publicacion pausada = publicacion(ana).publicadaEn(AHORA.plusMinutes(3)).guardar();
        pausada.pausar(ana.getId(), AHORA.plusMinutes(4));
        publicaciones.guardar(pausada);
        Publicacion p5 = publicacion(ana).publicadaEn(AHORA.plusMinutes(5)).guardar();

        List<Publicacion> todas = publicaciones.buscarPublicadas(10);
        List<Publicacion> acotadas = publicaciones.buscarPublicadas(2);

        assertIterableEquals(List.of(p5.getId(), p2.getId(), p1.getId()), todas.stream().map(Publicacion::getId).toList());
        assertIterableEquals(List.of(p5.getId(), p2.getId()), acotadas.stream().map(Publicacion::getId).toList());
        assertTrue(publicaciones.buscarPublicadas(0).isEmpty());
        assertEquals(1, todas.get(0).getFranjas().size(), "las colecciones vienen cargadas");
        assertEquals(2, todas.get(0).getHistorial().size());
    }
}
