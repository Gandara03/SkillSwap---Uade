package com.uade.skillswap.application.service;

import com.uade.skillswap.application.dto.PublicacionResumenDto;
import com.uade.skillswap.application.port.in.BusquedaUseCase;
import com.uade.skillswap.application.port.out.RelojPort;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.PerfilRepository;
import com.uade.skillswap.application.port.out.PromocionRepository;
import com.uade.skillswap.application.port.out.PublicacionRepository;

/** Busqueda paginada de publicaciones PUBLICADAS; autores cargados por lote (una consulta por pagina). */
public class BusquedaService implements BusquedaUseCase {

    private final PublicacionRepository publicacionRepository;
    private final ArmadorResumenes armador;

    public BusquedaService(PublicacionRepository publicacionRepository, PerfilRepository perfilRepository,
                           PromocionRepository promocionRepository, RelojPort reloj) {
        this.publicacionRepository = publicacionRepository;
        this.armador = new ArmadorResumenes(perfilRepository, new CalculadorPrecios(promocionRepository, reloj));
    }

    @Override
    public Pagina<PublicacionResumenDto> buscar(FiltroBusqueda filtro, PaginaRequest pagina) {
        Entradas.exigirPagina(pagina);
        FiltroBusqueda efectivo = filtro == null ? FiltroBusqueda.vacio() : filtro;
        Pagina<Publicacion> encontradas = publicacionRepository.buscar(efectivo, pagina);
        return new Pagina<>(armador.resumir(encontradas.contenido()), encontradas.pagina(), encontradas.tamano(),
                encontradas.totalElementos());
    }
}
