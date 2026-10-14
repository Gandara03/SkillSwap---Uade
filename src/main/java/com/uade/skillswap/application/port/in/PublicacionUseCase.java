package com.uade.skillswap.application.port.in;

import com.uade.skillswap.application.command.CrearPromocionCommand;
import com.uade.skillswap.application.command.CrearPublicacionCommand;
import com.uade.skillswap.application.command.EditarPublicacionCommand;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface PublicacionUseCase {

    PublicacionDto crearBorrador(Long actorId, CrearPublicacionCommand comando);

    PublicacionDto editar(Long actorId, Long publicacionId, EditarPublicacionCommand comando);

    PublicacionDto agregarFranja(Long actorId, Long publicacionId, LocalDateTime inicio, LocalDateTime fin);

    PublicacionDto publicar(Long actorId, Long publicacionId);

    PublicacionDto pausar(Long actorId, Long publicacionId);

    PublicacionDto reanudar(Long actorId, Long publicacionId);

    PublicacionDto cerrar(Long actorId, Long publicacionId);

    PublicacionDto cancelar(Long actorId, Long publicacionId, String motivo);

    /** Los borradores/pausadas solo los ve su autor. Incluye el precio final con promociones vigentes. */
    PublicacionDto obtener(Long actorId, Long publicacionId);

    Pagina<PublicacionResumenDto> listarPropias(Long actorId, PaginaRequest pagina);

    /** Vista publica: las publicaciones PUBLICADAS del autor, las mas recientes primero y como mucho {@code limite}. */
    List<PublicacionResumenDto> listarPublicadasDe(Long autorId, int limite);

    /** Solo el autor puede agregar promociones a su publicacion. */
    PublicacionDto agregarPromocion(Long actorId, CrearPromocionCommand comando);
}
