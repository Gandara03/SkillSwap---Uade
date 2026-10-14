package com.uade.skillswap.presentation.controller;

import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.EditarPublicacionCommand;
import com.uade.skillswap.application.command.FranjaCommand;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.application.port.in.PublicacionUseCase;
import com.uade.skillswap.application.port.in.SesionUseCase;
import com.uade.skillswap.domain.model.promocion.TipoPromocion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.presentation.Resultado;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

/** Adaptador delgado: valida la entrada y la traduce a PublicacionUseCase. Sin reglas de negocio. */
public class PublicacionController implements IPublicacionController {

    private static final String PUBLICACION = "la publicacion";

    private final PublicacionUseCase publicaciones;
    private final SesionUseCase sesion;

    public PublicacionController(PublicacionUseCase publicaciones, SesionUseCase sesion) {
        this.publicaciones = publicaciones;
        this.sesion = sesion;
    }

    @Override
    public Resultado<PublicacionDto> crearBorrador(CrearPublicacionCommand comando) {
        return Ejecucion.conActor(sesion, actor -> {
            validar(comando);
            return publicaciones.crearBorrador(actor, comando);
        });
    }

    @Override
    public Resultado<PublicacionDto> editar(Long publicacionId, EditarPublicacionCommand comando) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(publicacionId, PUBLICACION);
            validar(comando);
            return publicaciones.editar(actor, publicacionId, comando);
        });
    }

    @Override
    public Resultado<PublicacionDto> agregarFranja(Long publicacionId, LocalDateTime inicio, LocalDateTime fin) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(publicacionId, PUBLICACION);
            validarFranja(inicio, fin);
            return publicaciones.agregarFranja(actor, publicacionId, inicio, fin);
        });
    }

    @Override
    public Resultado<PublicacionDto> cambiarEstado(Long publicacionId, AccionPublicacion accion, String motivo) {
        if (accion == null) {
            return Ejecucion.conActor(sesion, actor -> {
                throw new ValidacionException("Falta indicar la accion");
            });
        }
        return switch (accion) {
            case PUBLICAR -> publicar(publicacionId);
            case PAUSAR -> pausar(publicacionId);
            case REANUDAR -> reanudar(publicacionId);
            case CERRAR -> cerrar(publicacionId);
            case CANCELAR -> cancelar(publicacionId, motivo);
        };
    }

    @Override
    public Resultado<PublicacionDto> publicar(Long publicacionId) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(publicacionId, PUBLICACION);
            return publicaciones.publicar(actor, publicacionId);
        });
    }

    @Override
    public Resultado<PublicacionDto> pausar(Long publicacionId) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(publicacionId, PUBLICACION);
            return publicaciones.pausar(actor, publicacionId);
        });
    }

    @Override
    public Resultado<PublicacionDto> reanudar(Long publicacionId) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(publicacionId, PUBLICACION);
            return publicaciones.reanudar(actor, publicacionId);
        });
    }

    @Override
    public Resultado<PublicacionDto> cerrar(Long publicacionId) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(publicacionId, PUBLICACION);
            return publicaciones.cerrar(actor, publicacionId);
        });
    }

    @Override
    public Resultado<PublicacionDto> cancelar(Long publicacionId, String motivo) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(publicacionId, PUBLICACION);
            ValidadorEntrada.exigirTexto(motivo, ValidadorEntrada.LARGO_MOTIVO, "el motivo de la cancelacion");
            return publicaciones.cancelar(actor, publicacionId, motivo);
        });
    }

    @Override
    public Resultado<PublicacionDto> obtener(Long publicacionId) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirId(publicacionId, PUBLICACION);
            return publicaciones.obtener(actor, publicacionId);
        });
    }

    @Override
    public Resultado<Pagina<PublicacionResumenDto>> misPublicaciones(PaginaRequest pagina) {
        return Ejecucion.conActor(sesion, actor -> {
            ValidadorEntrada.exigirPagina(pagina);
            return publicaciones.listarPropias(actor, pagina);
        });
    }

    @Override
    public Resultado<PublicacionDto> agregarPromocion(CrearPromocionCommand comando) {
        return Ejecucion.conActor(sesion, actor -> {
            validar(comando);
            return publicaciones.agregarPromocion(actor, comando);
        });
    }

    // ---------- validacion de entrada ----------

    private static void validar(CrearPublicacionCommand c) {
        ValidadorEntrada.exigir(c, "los datos de la publicacion");
        ValidadorEntrada.exigir(c.tipo(), "el tipo de publicacion");
        validarTextos(c.titulo(), c.descripcion(), c.categoria(), c.etiquetas(), c.habilidades());
        validarNumeros(c.duracionMinutos(), c.cupo(), c.precioBase());
        if (c.franjas() != null) {
            for (FranjaCommand franja : c.franjas()) {
                ValidadorEntrada.exigir(franja, "la franja");
                validarFranja(franja.inicio(), franja.fin());
            }
        }
    }

    private static void validar(EditarPublicacionCommand c) {
        ValidadorEntrada.exigir(c, "los datos de la publicacion");
        validarTextos(c.titulo(), c.descripcion(), c.categoria(), c.etiquetas(), c.habilidades());
        validarNumeros(c.duracionMinutos(), c.cupo(), c.precioBase());
    }

    /**
     * Titulo, categoria y precio pueden faltar: un borrador puede estar incompleto y la completitud la exige el
     * dominio al publicar. Lo que si se rechaza es un dato que nunca puede ser valido (demasiado largo o negativo).
     */
    private static void validarTextos(String titulo, String descripcion, String categoria, Set<String> etiquetas,
                                      Set<String> habilidades) {
        ValidadorEntrada.exigirLargo(titulo, ValidadorEntrada.LARGO_TITULO, "el titulo");
        ValidadorEntrada.exigirLargo(descripcion, ValidadorEntrada.LARGO_DESCRIPCION, "la descripcion");
        ValidadorEntrada.exigirLargo(categoria, ValidadorEntrada.LARGO_CATEGORIA, "la categoria");
        ValidadorEntrada.exigirTextos(etiquetas, ValidadorEntrada.LARGO_ITEM, "las etiquetas");
        ValidadorEntrada.exigirTextos(habilidades, ValidadorEntrada.LARGO_ITEM, "las habilidades");
    }

    private static void validarNumeros(int duracionMinutos, int cupo, BigDecimal precioBase) {
        ValidadorEntrada.exigirNoNegativo(duracionMinutos, "la duracion");
        ValidadorEntrada.exigirNoNegativo(cupo, "el cupo");
        ValidadorEntrada.exigirNoNegativo(precioBase, "el precio base");
    }

    private static void validarFranja(LocalDateTime inicio, LocalDateTime fin) {
        ValidadorEntrada.exigir(inicio, "el inicio de la franja");
        ValidadorEntrada.exigir(fin, "el fin de la franja");
        ValidadorEntrada.exigirAnterior(inicio, fin, "la franja");
    }

    private static void validar(CrearPromocionCommand c) {
        ValidadorEntrada.exigir(c, "los datos de la promocion");
        ValidadorEntrada.exigirId(c.publicacionId(), PUBLICACION);
        ValidadorEntrada.exigirTexto(c.descripcion(), ValidadorEntrada.LARGO_DESCRIPCION_PROMOCION,
                "la descripcion de la promocion");
        ValidadorEntrada.exigir(c.tipo(), "el tipo de promocion");
        ValidadorEntrada.exigirPositivo(c.valor(), "el valor de la promocion");
        if (c.tipo() == TipoPromocion.PORCENTAJE) {
            ValidadorEntrada.exigirMaximo(c.valor(), ValidadorEntrada.PORCENTAJE_MAXIMO, "el porcentaje");
        }
        ValidadorEntrada.exigirAnterior(c.vigenteDesde(), c.vigenteHasta(), "la vigencia de la promocion");
    }
}
