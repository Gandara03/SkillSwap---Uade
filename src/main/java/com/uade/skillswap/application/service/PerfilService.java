package com.uade.skillswap.application.service;

import com.uade.skillswap.application.command.ActualizarPerfilCommand;
import com.uade.skillswap.application.command.RegistrarPerfilCommand;
import com.uade.skillswap.application.config.SkillSwapConfig;
import com.uade.skillswap.application.dto.PerfilDto;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.application.mapper.Mapeador;
import com.uade.skillswap.application.port.in.PerfilUseCase;
import com.uade.skillswap.application.port.out.IdentidadPort;
import com.uade.skillswap.application.port.out.RelojPort;
import com.uade.skillswap.application.port.out.TransaccionPort;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.wallet.Wallet;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.PerfilRepository;
import com.uade.skillswap.application.port.out.PublicacionRepository;
import com.uade.skillswap.application.port.out.WalletRepository;

import java.time.LocalDateTime;

/** Alta, edicion y baja de perfiles. La vista publica la arma {@code PerfilPublicoFacade}. */
public class PerfilService implements PerfilUseCase {

    private final PerfilRepository perfilRepository;
    private final WalletRepository walletRepository;
    private final PublicacionRepository publicacionRepository;
    private final IdentidadPort identidad;
    private final TransaccionPort transaccion;
    private final RelojPort reloj;
    private final SkillSwapConfig config;

    public PerfilService(PerfilRepository perfilRepository, WalletRepository walletRepository,
                         PublicacionRepository publicacionRepository, IdentidadPort identidad,
                         TransaccionPort transaccion, RelojPort reloj, SkillSwapConfig config) {
        this.perfilRepository = perfilRepository;
        this.walletRepository = walletRepository;
        this.publicacionRepository = publicacionRepository;
        this.identidad = identidad;
        this.transaccion = transaccion;
        this.reloj = reloj;
        this.config = config;
    }

    @Override
    public PerfilDto registrar(RegistrarPerfilCommand comando) {
        Entradas.exigir(comando, "El comando");
        Entradas.exigirTexto(comando.userId(), "El userId");
        // El userId no lo elige el cliente: debe ser el del usuario autenticado en el servicio de identidad.
        IdentidadPort.UsuarioIdentidad usuario = identidad.usuarioActual()
                .orElseThrow(() -> new NoAutorizadoException("No hay una sesion iniciada"));
        if (!usuario.userId().equals(comando.userId().trim())) {
            throw new NoAutorizadoException("El userId no coincide con el usuario autenticado");
        }
        if (!usuario.activo()) {
            throw new NoAutorizadoException("El usuario esta inactivo en el servicio de identidad");
        }
        var zona = Entradas.zona(comando.zonaHoraria());
        var habilidades = Entradas.habilidades(comando.habilidades());
        var intereses = Entradas.intereses(comando.intereses());
        var disponibilidad = Entradas.bloques(comando.disponibilidad());

        return transaccion.ejecutar(() -> {
            if (perfilRepository.buscarPorUserId(usuario.userId()).isPresent()) {
                throw new ValidacionException("Ya existe un perfil para el usuario " + usuario.userId());
            }
            LocalDateTime ahora = reloj.ahora();
            Perfil perfil = Perfil.crear(usuario.userId(), comando.nombreVisible(), comando.tipo(), zona, ahora);
            perfil.actualizarDatos(comando.nombreVisible(), comando.biografia(), zona, habilidades, intereses,
                    disponibilidad, comando.canalPreferido());
            Perfil guardado = perfilRepository.guardar(perfil);
            // El perfil se guarda primero para conocer su id; el bono inicial entra como movimiento de la wallet.
            walletRepository.guardar(Wallet.abrir(guardado.getId(), config.bonoInicial(), ahora));
            return Mapeador.perfil(guardado);
        });
    }

    @Override
    public PerfilDto actualizar(Long actorId, ActualizarPerfilCommand comando) {
        Entradas.exigir(actorId, "El actor");
        Entradas.exigir(comando, "El comando");
        var zona = Entradas.zona(comando.zonaHoraria());
        var habilidades = Entradas.habilidades(comando.habilidades());
        var intereses = Entradas.intereses(comando.intereses());
        var disponibilidad = Entradas.bloques(comando.disponibilidad());

        return transaccion.ejecutar(() -> {
            Perfil perfil = Cargas.perfil(perfilRepository, actorId);
            perfil.actualizarDatos(comando.nombreVisible(), comando.biografia(), zona, habilidades, intereses,
                    disponibilidad, comando.canalPreferido());
            return Mapeador.perfil(perfilRepository.guardar(perfil));
        });
    }

    @Override
    public PerfilDto obtener(Long perfilId) {
        return Mapeador.perfil(Cargas.perfil(perfilRepository, perfilId));
    }

    @Override
    public void darDeBaja(Long actorId) {
        Entradas.exigir(actorId, "El actor");
        transaccion.ejecutar(() -> {
            Perfil perfil = Cargas.perfil(perfilRepository, actorId);
            perfil.darDeBaja();
            perfilRepository.guardar(perfil);
            pausarPublicacionesActivas(actorId, reloj.ahora());
        });
    }

    /** Un perfil dado de baja no debe seguir recibiendo solicitudes: se pausan sus publicaciones publicadas. */
    private void pausarPublicacionesActivas(Long autorId, LocalDateTime ahora) {
        int numeroPagina = 0;
        Pagina<Publicacion> pagina;
        do {
            pagina = publicacionRepository.buscarPorAutor(autorId,
                    PaginaRequest.de(numeroPagina++, PaginaRequest.TAMANO_MAXIMO));
            for (Publicacion publicacion : pagina.contenido()) {
                if (publicacion.estaPublicada()) {
                    publicacion.pausar(autorId, ahora);
                    publicacionRepository.guardar(publicacion);
                }
            }
        } while (pagina.tieneSiguiente());
    }
}
