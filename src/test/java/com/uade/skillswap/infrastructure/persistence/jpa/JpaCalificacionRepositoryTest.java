package com.uade.skillswap.infrastructure.persistence.jpa;

import com.uade.skillswap.domain.model.calificacion.Calificacion;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.vo.Puntaje;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaCalificacionRepositoryTest extends JpaTestBase {

    private Perfil proveedor;
    private Perfil cliente;
    private Publicacion publicacion;
    private Contratacion completada;

    @BeforeEach
    void crearEscenario() {
        proveedor = profesional("u-ana");
        cliente = profesional("u-bruno");
        publicacion = publicacion(proveedor).guardar();
        completada = completada(publicacion, cliente);
    }

    private Calificacion calificar(Contratacion c, Perfil autor, int puntaje, String comentario, int minutos) {
        return Calificacion.registrar(c, autor.getId(), Puntaje.de(puntaje), comentario, AHORA.plusMinutes(minutos));
    }

    @Test
    @DisplayName("guardar y recuperar una calificacion: el destinatario es la contraparte del autor")
    void guardarYRecuperar() {
        Calificacion guardada = calificaciones.guardar(calificar(completada, cliente, 5, "Excelente", 10));

        assertNotNull(guardada.getId());
        List<Calificacion> recibidas = calificaciones.buscarPorDestinatario(proveedor.getId());
        assertEquals(1, recibidas.size());
        Calificacion leida = recibidas.get(0);
        assertEquals(guardada.getId(), leida.getId());
        assertEquals(completada.getId(), leida.getContratacionId());
        assertEquals(cliente.getId(), leida.getAutorId());
        assertEquals(proveedor.getId(), leida.getDestinatarioId());
        assertEquals(5, leida.getPuntaje().getValor());
        assertEquals("Excelente", leida.getComentario());
        assertEquals(AHORA.plusMinutes(10), leida.getFecha());
        assertTrue(calificaciones.buscarPorDestinatario(cliente.getId()).isEmpty());
    }

    @Test
    @DisplayName("existe(contratacion, autor) distingue quien ya califico")
    void existe() {
        calificaciones.guardar(calificar(completada, cliente, 4, null, 10));

        assertTrue(calificaciones.existe(completada.getId(), cliente.getId()));
        assertFalse(calificaciones.existe(completada.getId(), proveedor.getId()));
        assertFalse(calificaciones.existe(9999L, cliente.getId()));
    }

    @Test
    @DisplayName("restriccion unica (contratacion, autor): no se puede calificar dos veces la misma contratacion")
    void unicaPorContratacionYAutor() {
        calificaciones.guardar(calificar(completada, cliente, 5, "Muy bueno", 10));

        assertThrows(PersistenceException.class,
                () -> calificaciones.guardar(calificar(completada, cliente, 1, "Cambie de opinion", 11)));
        assertEquals(1, calificaciones.buscarPorDestinatario(proveedor.getId()).size());
    }

    @Test
    @DisplayName("ambos participantes pueden calificarse mutuamente")
    void ambosCalifican() {
        calificaciones.guardar(calificar(completada, cliente, 5, "Gran mentor", 10));
        calificaciones.guardar(calificar(completada, proveedor, 4, "Buen alumno", 11));

        assertEquals(1, calificaciones.buscarPorDestinatario(proveedor.getId()).size());
        assertEquals(1, calificaciones.buscarPorDestinatario(cliente.getId()).size());
        assertEquals(4, calificaciones.buscarPorDestinatario(cliente.getId()).get(0).getPuntaje().getValor());
    }

    @Test
    @DisplayName("las calificaciones son inmutables: guardar una ya guardada no inserta otra fila")
    void guardarDosVecesNoDuplica() {
        Calificacion c = calificar(completada, cliente, 5, "Excelente", 10);
        calificaciones.guardar(c);
        calificaciones.guardar(c);

        assertEquals(1, calificaciones.buscarPorDestinatario(proveedor.getId()).size());
    }

    @Test
    @DisplayName("paginarPorDestinatario ordena de la mas reciente a la mas antigua y pagina con total")
    void paginarPorDestinatario() {
        Calificacion c1 = calificaciones.guardar(calificar(completada, cliente, 3, "Una", 10));
        Contratacion otra = completada(publicacion, profesional("u-cecilia"));
        Perfil autor2 = perfiles.buscarPorId(otra.getClienteId()).orElseThrow();
        Calificacion c2 = calificaciones.guardar(calificar(otra, autor2, 4, "Dos", 20));
        Contratacion tercera = completada(publicacion, profesional("u-diego"));
        Perfil autor3 = perfiles.buscarPorId(tercera.getClienteId()).orElseThrow();
        Calificacion c3 = calificaciones.guardar(calificar(tercera, autor3, 5, "Tres", 30));

        Pagina<Calificacion> primera = calificaciones.paginarPorDestinatario(proveedor.getId(), PaginaRequest.de(0, 2));
        Pagina<Calificacion> segunda = calificaciones.paginarPorDestinatario(proveedor.getId(), PaginaRequest.de(1, 2));

        assertEquals(3, primera.totalElementos());
        assertIterableEquals(List.of(c3.getId(), c2.getId()), primera.contenido().stream().map(Calificacion::getId).toList());
        assertIterableEquals(List.of(c1.getId()), segunda.contenido().stream().map(Calificacion::getId).toList());
        assertIterableEquals(List.of(c3.getId(), c2.getId(), c1.getId()),
                calificaciones.buscarPorDestinatario(proveedor.getId()).stream().map(Calificacion::getId).toList());
        assertEquals(0, calificaciones.paginarPorDestinatario(9999L, PaginaRequest.de(0, 5)).totalElementos());
    }
}
