package com.uade.skillswap.application.port.out;

import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.query.FiltroBusqueda;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;

import java.util.List;
import java.util.Optional;

public interface PublicacionRepository {
    Publicacion guardar(Publicacion publicacion);
    Optional<Publicacion> buscarPorId(Long id);
    Pagina<Publicacion> buscar(FiltroBusqueda filtro, PaginaRequest pagina);
    Pagina<Publicacion> buscarPorAutor(Long autorId, PaginaRequest pagina);
    /** Publicaciones publicadas, acotadas, para alimentar las estrategias de recomendacion. */
    List<Publicacion> buscarPublicadas(int limite);
}
