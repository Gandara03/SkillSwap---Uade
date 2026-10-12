package com.uade.skillswap.application.service;

import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.EditarPublicacionCommand;
import com.uade.skillswap.application.command.FranjaCommand;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.mapper.Mapeador;
import com.uade.skillswap.application.port.in.PublicacionUseCase;
import com.uade.skillswap.application.port.out.RelojPort;
import com.uade.skillswap.application.port.out.TransaccionPort;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.application.precio.PrecioPublicacion;
import com.uade.skillswap.domain.model.promocion.Promocion;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.PerfilRepository;
import com.uade.skillswap.application.port.out.PromocionRepository;
import com.uade.skillswap.application.port.out.PublicacionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Ciclo de vida de publicaciones. Delgado: valida permisos (solo el autor), carga, delega la regla en el
 * agregado (State, franjas, completitud) y guarda, todo en una transaccion.
 */
public class PublicacionService implements PublicacionUseCase {

    private final PublicacionRepository publicacionRepository;
    private final PerfilRepository perfilRepository;
    private final PromocionRepository promocionRepository;
    private final TransaccionPort transaccion;
    private final RelojPort reloj;
    private final CalculadorPrecios precios;

    public PublicacionService(PublicacionRepository publicacionRepository, PerfilRepository perfilRepository,
                              PromocionRepository promocionRepository, TransaccionPort transaccion,
                              RelojPort reloj) {
        this.publicacionRepository = publicacionRepository;
        this.perfilRepository = perfilRepository;
        this.promocionRepository = promocionRepository;
        this.transaccion = transaccion;
        this.reloj = reloj;
        this.precios = new CalculadorPrecios(promocionRepository, reloj);
    }

    @Override
    public PublicacionDto crearBorrador(Long actorId, CrearPublicacionCommand comando) {
        Entradas.exigir(actorId, "El actor");
        Entradas.exigir(comando, "El comando");
        var habilidades = Entradas.habilidades(comando.habilidades());
        List<FranjaCommand> franjas = comando.franjas() == null ? List.of() : comando.franjas();

        return transaccion.ejecutar(() -> {
            Perfil autor = Cargas.perfilActivo(perfilRepository, actorId);
            LocalDateTime ahora = reloj.ahora();
            Publicacion borrador = Publicacion.crearBorrador(comando.tipo(), actorId, autor.getTipo(),
                    comando.titulo(), comando.descripcion(), comando.categoria(), comando.modalidad(),
                    comando.duracionMinutos(), comando.cupo(), Entradas.precio(comando.precioBase()),
                    comando.etiquetas(), habilidades, comando.politicaCancelacion(), ahora);
            for (FranjaCommand franja : franjas) {
                Entradas.exigir(franja, "La franja");
                borrador.agregarFranja(franja.inicio(), franja.fin(), ahora);
            }
            return dto(publicacionRepository.guardar(borrador));
        });
    }

    @Override
    public PublicacionDto editar(Long actorId, Long publicacionId, EditarPublicacionCommand comando) {
        Entradas.exigir(comando, "El comando");
        var habilidades = Entradas.habilidades(comando.habilidades());
        return modificar(actorId, publicacionId, (p, ahora) -> p.editar(comando.titulo(), comando.descripcion(),
                comando.categoria(), comando.modalidad(), comando.duracionMinutos(), comando.cupo(),
                Entradas.precio(comando.precioBase()), comando.etiquetas(), habilidades,
                comando.politicaCancelacion()));
    }

    @Override
    public PublicacionDto agregarFranja(Long actorId, Long publicacionId, LocalDateTime inicio, LocalDateTime fin) {
        Entradas.exigir(inicio, "El inicio de la franja");
        Entradas.exigir(fin, "El fin de la franja");
        return modificar(actorId, publicacionId, (p, ahora) -> p.agregarFranja(inicio, fin, ahora));
    }

    @Override
    public PublicacionDto publicar(Long actorId, Long publicacionId) {
        return modificar(actorId, publicacionId, (p, ahora) -> p.publicar(actorId, ahora));
    }

    @Override
    public PublicacionDto pausar(Long actorId, Long publicacionId) {
        return modificar(actorId, publicacionId, (p, ahora) -> p.pausar(actorId, ahora));
    }

    @Override
    public PublicacionDto reanudar(Long actorId, Long publicacionId) {
        return modificar(actorId, publicacionId, (p, ahora) -> p.reanudar(actorId, ahora));
    }

    @Override
    public PublicacionDto cerrar(Long actorId, Long publicacionId) {
        return modificar(actorId, publicacionId, (p, ahora) -> p.cerrar(actorId, ahora));
    }

    @Override
    public PublicacionDto cancelar(Long actorId, Long publicacionId, String motivo) {
        return modificar(actorId, publicacionId, (p, ahora) -> p.cancelar(actorId, motivo, ahora));
    }

    @Override
    public PublicacionDto obtener(Long actorId, Long publicacionId) {
        Publicacion publicacion = Cargas.publicacion(publicacionRepository, publicacionId);
        if (!publicacion.esVisiblePara(actorId)) {
            throw new NoAutorizadoException("La publicacion no es visible para este perfil");
        }
        return dto(publicacion);
    }

    @Override
    public Pagina<PublicacionResumenDto> listarPropias(Long actorId, PaginaRequest pagina) {
        Entradas.exigirPagina(pagina);
        Perfil autor = Cargas.perfil(perfilRepository, actorId);
        return publicacionRepository.buscarPorAutor(actorId, pagina)
                .mapear(p -> Mapeador.resumen(p, autor, precios.para(p)));
    }

    @Override
    public PublicacionDto agregarPromocion(Long actorId, CrearPromocionCommand comando) {
        Entradas.exigir(comando, "El comando");
        Entradas.exigir(comando.publicacionId(), "La publicacion");
        Entradas.exigirTexto(comando.descripcion(), "La descripcion de la promocion");
        return transaccion.ejecutar(() -> {
            Publicacion publicacion = cargarComoAutor(actorId, comando.publicacionId());
            promocionRepository.guardar(Promocion.crear(publicacion.getId(), comando.descripcion(), comando.tipo(),
                    comando.valor(), comando.vigenteDesde(), comando.vigenteHasta()));
            return dto(publicacion);
        });
    }

    /** Patron comun de las operaciones del autor: cargar, verificar autoria, mutar el agregado, guardar. */
    private PublicacionDto modificar(Long actorId, Long publicacionId, BiConsumer<Publicacion, LocalDateTime> cambio) {
        return transaccion.ejecutar(() -> {
            Publicacion publicacion = cargarComoAutor(actorId, publicacionId);
            cambio.accept(publicacion, reloj.ahora());
            return dto(publicacionRepository.guardar(publicacion));
        });
    }

    private Publicacion cargarComoAutor(Long actorId, Long publicacionId) {
        Entradas.exigir(actorId, "El actor");
        Publicacion publicacion = Cargas.publicacion(publicacionRepository, publicacionId);
        if (!publicacion.esAutor(actorId)) {
            throw new NoAutorizadoException("Solo el autor puede modificar la publicacion");
        }
        return publicacion;
    }

    private PublicacionDto dto(Publicacion publicacion) {
        PrecioPublicacion precio = precios.para(publicacion);
        return Mapeador.publicacion(publicacion, precio);
    }
}
