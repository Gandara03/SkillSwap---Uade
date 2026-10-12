package com.uade.skillswap.acceptance.memoria;

import com.uade.skillswap.application.command.CalificarCommand;
import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.FranjaCommand;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
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

/**
 * Armado comun de los casos de aceptacion en memoria. Todo pasa por los casos de uso reales de
 * {@link AplicacionEnMemoria} (servicios + repositorios fake + adapters de notificacion con SDK mock);
 * esta clase solo evita repetir el "dar de alta usuarios, publicar y contratar" en cada caso.
 */
final class EscenarioMemoria {

    final AplicacionEnMemoria app = AplicacionEnMemoria.crear();

    LocalDateTime ahora() {
        return app.reloj.ahora();
    }

    // ---------- perfiles ----------

    /** Da de alta el usuario en la identidad fake y registra su perfil PROFESIONAL (el wallet nace con el bono de 100 SC). */
    PerfilDto profesional(String userId, String nombre) {
        return registrar(userId, nombre, TipoPerfil.PROFESIONAL, CanalNotificacion.INTERNA, Set.of(), Set.of());
    }

    PerfilDto profesional(String userId, String nombre, CanalNotificacion canal) {
        return registrar(userId, nombre, TipoPerfil.PROFESIONAL, canal, Set.of(), Set.of());
    }

    PerfilDto registrar(String userId, String nombre, TipoPerfil tipo, CanalNotificacion canal,
                        Set<String> habilidades, Set<String> intereses) {
        app.identidad.agregarUsuario(userId, nombre);
        app.identidad.iniciarSesion(userId);
        return app.perfilUseCase.registrar(new RegistrarPerfilCommand(userId, nombre, tipo, "UTC", null,
                habilidades, intereses, List.of(), canal));
    }

    BigDecimal saldo(Long perfilId) {
        return app.walletUseCase.consultar(perfilId).saldo();
    }

    // ---------- publicaciones ----------

    /** Una franja de una hora que empieza dentro de {@code desdeAhora}. */
    FranjaCommand franja(Duration desdeAhora) {
        LocalDateTime inicio = ahora().plus(desdeAhora);
        return new FranjaCommand(inicio, inicio.plusHours(1));
    }

    /** Franjas diarias: dentro de 3, 4, 5... dias (la cantidad pedida). */
    List<FranjaCommand> franjasDiarias(int cantidad) {
        List<FranjaCommand> franjas = new ArrayList<>();
        for (int i = 0; i < cantidad; i++) {
            franjas.add(franja(Duration.ofDays(3L + i)));
        }
        return franjas;
    }

    CrearPublicacionCommand mentoria(String titulo, String categoria, long precio, int cupo,
                                     TipoPoliticaCancelacion politica, Set<String> etiquetas, Set<String> habilidades,
                                     List<FranjaCommand> franjas) {
        return new CrearPublicacionCommand(TipoPublicacion.MENTORIA, titulo, "Descripcion de " + titulo, categoria,
                Modalidad.REMOTA, 60, cupo, BigDecimal.valueOf(precio), etiquetas, habilidades, politica, franjas);
    }

    /** Crea el borrador y lo publica, ambos por caso de uso. */
    PublicacionDto publicar(Long autorId, CrearPublicacionCommand comando) {
        PublicacionDto borrador = app.publicacionUseCase.crearBorrador(autorId, comando);
        return app.publicacionUseCase.publicar(autorId, borrador.id());
    }

    /** Mentoria simple publicada: categoria "programacion", politica FLEXIBLE y {@code cantidadFranjas} franjas diarias. */
    PublicacionDto publicarMentoria(Long autorId, long precio, int cupo, int cantidadFranjas) {
        return publicar(autorId, mentoria("Mentoria de Java", "programacion", precio, cupo,
                TipoPoliticaCancelacion.FLEXIBLE, Set.of("java"), Set.of("java"), franjasDiarias(cantidadFranjas)));
    }

    PublicacionDto promocionar(Long autorId, Long publicacionId, TipoPromocion tipo, long valor) {
        return app.publicacionUseCase.agregarPromocion(autorId, new CrearPromocionCommand(publicacionId,
                "Promo " + tipo + " " + valor, tipo, BigDecimal.valueOf(valor), null, null));
    }

    // ---------- contrataciones ----------

    ContratacionDto solicitar(Long clienteId, PublicacionDto publicacion, int indiceFranja) {
        return app.contratacionUseCase.solicitar(clienteId, new SolicitarContratacionCommand(publicacion.id(),
                publicacion.franjas().get(indiceFranja).id(), "Hola, quiero la mentoria", "Aprender patrones"));
    }

    /** Solicitar + aceptar + pagar; devuelve la contratacion CONFIRMADA. */
    ContratacionDto contratarYPagar(Long proveedorId, Long clienteId, PublicacionDto publicacion, int indiceFranja) {
        ContratacionDto solicitud = solicitar(clienteId, publicacion, indiceFranja);
        app.contratacionUseCase.aceptar(proveedorId, solicitud.id());
        return app.contratacionUseCase.pagar(clienteId, solicitud.id());
    }

    /** Recorre el flujo completo hasta COMPLETADA. */
    ContratacionDto contratarYCompletar(Long proveedorId, Long clienteId, PublicacionDto publicacion, int indiceFranja) {
        ContratacionDto pagada = contratarYPagar(proveedorId, clienteId, publicacion, indiceFranja);
        app.contratacionUseCase.iniciar(proveedorId, pagada.id());
        return app.contratacionUseCase.completar(proveedorId, pagada.id());
    }

    CalificarCommand calificacion(Long contratacionId, int puntaje) {
        return new CalificarCommand(contratacionId, puntaje, "Puntaje " + puntaje);
    }

    PaginaRequest primeraPagina() {
        return PaginaRequest.de(0, 20);
    }
}
