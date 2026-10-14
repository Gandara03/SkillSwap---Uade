package com.uade.skillswap.application.service;

import com.uade.skillswap.application.command.SolicitarContratacionCommand;
import com.uade.skillswap.application.dto.ComprobanteDto;
import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.HistorialDto;
import com.uade.skillswap.application.dto.ResultadoCancelacionDto;
import com.uade.skillswap.application.exception.NoAutorizadoException;
import com.uade.skillswap.application.exception.RecursoNoEncontradoException;
import com.uade.skillswap.application.mapper.Mapeador;
import com.uade.skillswap.application.port.in.ContratacionUseCase;
import com.uade.skillswap.application.port.out.RelojPort;
import com.uade.skillswap.application.port.out.TransaccionPort;
import com.uade.skillswap.domain.exception.FranjaNoDisponibleException;
import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.exception.SaldoInsuficienteException;
import com.uade.skillswap.domain.model.contratacion.Contratacion;
import com.uade.skillswap.domain.model.contratacion.PropuestaReprogramacion;
import com.uade.skillswap.domain.model.contratacion.RepartoCancelacion;
import com.uade.skillswap.domain.model.contratacion.observer.ContratacionObserver;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.politica.PoliticaCancelacion;
import com.uade.skillswap.domain.model.politica.PoliticaCancelacionFactory;
import com.uade.skillswap.domain.model.precio.PrecioPublicacion;
import com.uade.skillswap.domain.model.publicacion.Franja;
import com.uade.skillswap.domain.model.publicacion.Publicacion;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.domain.model.wallet.TipoMovimiento;
import com.uade.skillswap.domain.model.wallet.Wallet;
import com.uade.skillswap.domain.query.Pagina;
import com.uade.skillswap.domain.query.PaginaRequest;
import com.uade.skillswap.application.port.out.ContratacionRepository;
import com.uade.skillswap.application.port.out.PerfilRepository;
import com.uade.skillswap.application.port.out.PromocionRepository;
import com.uade.skillswap.application.port.out.PublicacionRepository;
import com.uade.skillswap.application.port.out.WalletRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orquesta el ciclo de la contratacion. Cada operacion que muta corre en UNA transaccion: cambio de estado
 * (State en el agregado), franja/cupo (Publicacion) y movimientos de wallet (escrow). Los eventos (Observer)
 * se despachan recien despues del commit. La regla vive en el dominio; aca solo se cargan, ejecutan y guardan.
 */
public class ContratacionService implements ContratacionUseCase {

    private static final Logger LOG = Logger.getLogger(ContratacionService.class.getName());

    private final ContratacionRepository contratacionRepository;
    private final PublicacionRepository publicacionRepository;
    private final PerfilRepository perfilRepository;
    private final WalletRepository walletRepository;
    private final List<ContratacionObserver> observers;
    private final TransaccionPort transaccion;
    private final RelojPort reloj;
    private final CalculadorPrecios precios;
    private final PoliticaCancelacionFactory politicas;

    /**
     * {@code observers}: los interesados en los eventos de las contrataciones. {@code politicas}: crea la politica
     * de cancelacion de cada publicacion con los umbrales configurados. Las dos cosas las arma el compositor.
     */
    public ContratacionService(ContratacionRepository contratacionRepository,
                               PublicacionRepository publicacionRepository, PerfilRepository perfilRepository,
                               WalletRepository walletRepository, PromocionRepository promocionRepository,
                               List<ContratacionObserver> observers, TransaccionPort transaccion, RelojPort reloj,
                               PoliticaCancelacionFactory politicas) {
        this.contratacionRepository = contratacionRepository;
        this.publicacionRepository = publicacionRepository;
        this.perfilRepository = perfilRepository;
        this.walletRepository = walletRepository;
        this.observers = List.copyOf(observers);
        this.transaccion = transaccion;
        this.reloj = reloj;
        this.precios = new CalculadorPrecios(promocionRepository, reloj);
        this.politicas = politicas;
    }

    // ---------- operaciones que mutan ----------

    @Override
    public ContratacionDto solicitar(Long actorId, SolicitarContratacionCommand comando) {
        Entradas.exigir(actorId, "El actor");
        Entradas.exigir(comando, "El comando");
        Entradas.exigir(comando.publicacionId(), "La publicacion");
        Entradas.exigir(comando.franjaId(), "La franja");

        return operar(() -> {
            LocalDateTime ahora = reloj.ahora();
            Cargas.perfilActivo(perfilRepository, actorId);
            Publicacion publicacion = Cargas.publicacion(publicacionRepository, comando.publicacionId());
            publicacion.validarSolicitud(actorId, comando.franjaId(), ahora);
            Perfil proveedor = Cargas.perfil(perfilRepository, publicacion.getAutorId());
            if (!proveedor.estaActivo()) {
                throw new ReglaDeNegocioException("El autor de la publicacion ya no esta activo");
            }
            PrecioPublicacion precio = precios.para(publicacion, ahora);
            if (precio == null) {
                throw new ReglaDeNegocioException("La publicacion no tiene precio definido");
            }
            Contratacion solicitud = Contratacion.solicitar(publicacion.getId(), actorId, publicacion.getAutorId(),
                    comando.franjaId(), comando.mensaje(), comando.objetivo(), precio.calcular(), ahora);
            Contratacion guardada = contratacionRepository.guardar(solicitud);
            return cambio(solicitud, guardada, publicacion.getTitulo());
        });
    }

    @Override
    public ContratacionDto aceptar(Long actorId, Long contratacionId) {
        return operar(() -> {
            Contratacion contratacion = cargarComoProveedor(actorId, contratacionId);
            Publicacion publicacion = Cargas.publicacion(publicacionRepository, contratacion.getPublicacionId());
            LocalDateTime ahora = reloj.ahora();
            contratacion.aceptar(actorId, ahora);
            publicacion.reservarFranja(contratacion.getFranjaId(), ahora); // franja y cupo, misma transaccion
            Contratacion guardada = contratacionRepository.guardar(contratacion);
            publicacionRepository.guardar(publicacion);
            return cambio(contratacion, guardada, publicacion.getTitulo());
        });
    }

    @Override
    public ContratacionDto rechazar(Long actorId, Long contratacionId, String motivo) {
        return operar(() -> {
            Contratacion contratacion = cargarComoProveedor(actorId, contratacionId);
            contratacion.rechazar(actorId, motivo, reloj.ahora());
            return cambio(contratacion, contratacionRepository.guardar(contratacion));
        });
    }

    @Override
    public ContratacionDto pagar(Long actorId, Long contratacionId) {
        return operar(() -> {
            Contratacion contratacion = cargarComoCliente(actorId, contratacionId);
            LocalDateTime ahora = reloj.ahora();
            // Escrow: se debita al cliente y el monto queda retenido en la contratacion (no hay wallet de plataforma).
            Wallet billetera = Cargas.wallet(walletRepository, contratacion.getClienteId());
            // Se valida el saldo ANTES de mutar la contratacion: si no alcanza, no cambia nada.
            if (billetera.getSaldo().esMenorQue(contratacion.getPrecioFinal())) {
                throw new SaldoInsuficienteException("Saldo insuficiente: disponible " + billetera.getSaldo()
                        + ", requerido " + contratacion.getPrecioFinal());
            }
            contratacion.pagar(actorId, ahora);
            if (!contratacion.getPrecioFinal().esCero()) {
                billetera.debitar(contratacion.getPrecioFinal(), TipoMovimiento.PAGO, contratacion.getId(), ahora);
            }
            Contratacion guardada = contratacionRepository.guardar(contratacion);
            walletRepository.guardar(billetera);
            return cambio(contratacion, guardada);
        });
    }

    @Override
    public ContratacionDto iniciar(Long actorId, Long contratacionId) {
        return operar(() -> {
            Contratacion contratacion = cargarComoProveedor(actorId, contratacionId);
            contratacion.iniciar(actorId, reloj.ahora());
            return cambio(contratacion, contratacionRepository.guardar(contratacion));
        });
    }

    @Override
    public ContratacionDto completar(Long actorId, Long contratacionId) {
        return operar(() -> {
            Contratacion contratacion = cargarComoProveedor(actorId, contratacionId);
            LocalDateTime ahora = reloj.ahora();
            contratacion.completar(actorId, ahora);
            Contratacion guardada = contratacionRepository.guardar(contratacion);
            acreditar(contratacion.getProveedorId(), contratacion.getPrecioFinal(), TipoMovimiento.COBRO,
                    contratacion.getId(), ahora); // se libera al proveedor lo retenido
            return cambio(contratacion, guardada);
        });
    }

    @Override
    public ResultadoCancelacionDto cancelar(Long actorId, Long contratacionId, String motivo) {
        return operar(() -> {
            Contratacion contratacion = cargarComoParticipante(actorId, contratacionId);
            Publicacion publicacion = Cargas.publicacion(publicacionRepository, contratacion.getPublicacionId());
            LocalDateTime ahora = reloj.ahora();
            Franja franja = publicacion.buscarFranja(contratacion.getFranjaId()).orElseThrow(
                    () -> new RecursoNoEncontradoException("Franja", contratacion.getFranjaId()));
            LocalDateTime inicioFranja = franja.getInicio();

            contratacion.cancelar(actorId, motivo, ahora);
            publicacion.liberarFranja(contratacion.getFranjaId()); // devuelve franja y cupo

            // El dominio decide cuanto vuelve al cliente y cuanto compensa al proveedor (escrow + politica).
            PoliticaCancelacion politica = politicas.crear(publicacion.getPoliticaCancelacion());
            RepartoCancelacion reparto = contratacion.repartirRetenido(politica,
                    Duration.between(ahora, inicioFranja));

            Contratacion guardada = contratacionRepository.guardar(contratacion);
            publicacionRepository.guardar(publicacion);
            acreditar(contratacion.getClienteId(), reparto.getReintegro(), TipoMovimiento.REINTEGRO,
                    contratacion.getId(), ahora);
            acreditar(contratacion.getProveedorId(), reparto.getCompensacion(), TipoMovimiento.COBRO,
                    contratacion.getId(), ahora);

            ContratacionDto dto = Mapeador.contratacion(guardada, publicacion.getTitulo());
            return new Cambio<>(contratacion, new ResultadoCancelacionDto(dto, politica.getTipo().name(),
                    Mapeador.monto(reparto.getReintegro()), Mapeador.monto(reparto.getCompensacion())));
        });
    }

    @Override
    public ContratacionDto proponerReprogramacion(Long actorId, Long contratacionId, Long nuevaFranjaId) {
        return operar(() -> {
            Contratacion contratacion = cargarComoParticipante(actorId, contratacionId);
            Publicacion publicacion = Cargas.publicacion(publicacionRepository, contratacion.getPublicacionId());
            LocalDateTime ahora = reloj.ahora();
            contratacion.proponerReprogramacion(actorId, nuevaFranjaId, ahora);
            exigirFranjaDisponible(publicacion, nuevaFranjaId, ahora);
            return cambio(contratacion, contratacionRepository.guardar(contratacion), publicacion.getTitulo());
        });
    }

    @Override
    public ContratacionDto responderReprogramacion(Long actorId, Long contratacionId, boolean acepta) {
        return operar(() -> {
            Contratacion contratacion = cargarComoParticipante(actorId, contratacionId);
            PropuestaReprogramacion propuesta = contratacion.getPropuestaReprogramacion();
            if (propuesta != null && propuesta.getPropuestaPorId().equals(actorId)) {
                throw new NoAutorizadoException("La propuesta debe ser respondida por la otra parte");
            }
            Publicacion publicacion = Cargas.publicacion(publicacionRepository, contratacion.getPublicacionId());
            LocalDateTime ahora = reloj.ahora();
            Long franjaAnterior = contratacion.getFranjaId();
            contratacion.responderReprogramacion(actorId, acepta, ahora);
            if (acepta) {
                // La reserva se mueve de franja sin alterar el cupo; si la nueva se ocupo, falla y se revierte todo.
                publicacion.reprogramarReserva(franjaAnterior, contratacion.getFranjaId(), ahora);
                publicacionRepository.guardar(publicacion);
            }
            return cambio(contratacion, contratacionRepository.guardar(contratacion), publicacion.getTitulo());
        });
    }

    // ---------- consultas ----------

    @Override
    public ContratacionDto obtener(Long actorId, Long contratacionId) {
        Contratacion contratacion = cargarComoParticipante(actorId, contratacionId);
        return Mapeador.contratacion(contratacion, titulo(contratacion.getPublicacionId()));
    }

    @Override
    public ComprobanteDto comprobante(Long actorId, Long contratacionId) {
        return Mapeador.comprobante(cargarComoParticipante(actorId, contratacionId).consultarComprobante());
    }

    @Override
    public Pagina<HistorialDto> historial(Long actorId, Long contratacionId, PaginaRequest pagina) {
        Entradas.exigirPagina(pagina);
        // el historial viene completo con el agregado (que igual se carga para validar al participante)
        return Pagina.recortar(Mapeador.historial(cargarComoParticipante(actorId, contratacionId).getHistorial()),
                pagina);
    }

    @Override
    public Pagina<ContratacionDto> listarMias(Long actorId, PaginaRequest pagina) {
        Entradas.exigir(actorId, "El actor");
        Entradas.exigirPagina(pagina);
        Map<Long, String> titulos = new HashMap<>(); // una lectura por publicacion distinta de la pagina
        return contratacionRepository.buscarPorParticipante(actorId, pagina)
                .mapear(c -> Mapeador.contratacion(c, titulos.computeIfAbsent(c.getPublicacionId(), this::titulo)));
    }

    // ---------- soporte ----------

    /** Resultado de una operacion: la contratacion operada (con sus eventos pendientes) y lo que se devuelve. */
    private record Cambio<T>(Contratacion contratacion, T resultado) { }

    private Cambio<ContratacionDto> cambio(Contratacion original, Contratacion guardada) {
        return cambio(original, guardada, titulo(guardada.getPublicacionId()));
    }

    private Cambio<ContratacionDto> cambio(Contratacion original, Contratacion guardada, String titulo) {
        return new Cambio<>(original, Mapeador.contratacion(guardada, titulo));
    }

    /** Ejecuta el trabajo en una transaccion y, una vez confirmada, notifica los eventos acumulados. */
    private <T> T operar(Supplier<Cambio<T>> trabajo) {
        Cambio<T> cambio = transaccion.ejecutar(trabajo);
        despachar(cambio.contratacion());
        return cambio.resultado();
    }

    /**
     * Suscribe a la contratacion los observers que armo el compositor y publica sus eventos pendientes, ya fuera
     * de la transaccion. Se suscriben en cada operacion porque cada una trabaja con una instancia rehidratada.
     * El repositorio le asigna el id al guardar, asi que los eventos de una solicitud nueva ya salen con su id.
     * Una falla de notificacion no deshace una operacion ya confirmada: se registra y se sigue.
     */
    private void despachar(Contratacion contratacion) {
        for (ContratacionObserver observer : observers) {
            contratacion.agregarObserver(observer);
        }
        try {
            contratacion.notificarEventosPendientes();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "No se pudieron notificar los eventos de la contratacion "
                    + contratacion.getId(), e);
        }
    }

    /** Acredita en la wallet del perfil; un monto cero no genera movimiento (la wallet exige monto positivo). */
    private void acreditar(Long perfilId, SkillCoins monto, TipoMovimiento tipo, Long contratacionId,
                           LocalDateTime ahora) {
        if (monto.esCero()) {
            return;
        }
        Wallet billetera = Cargas.wallet(walletRepository, perfilId);
        billetera.acreditar(monto, tipo, contratacionId, ahora);
        walletRepository.guardar(billetera);
    }

    private void exigirFranjaDisponible(Publicacion publicacion, Long franjaId, LocalDateTime ahora) {
        Franja franja = publicacion.buscarFranja(franjaId)
                .orElseThrow(() -> new FranjaNoDisponibleException("La franja " + franjaId + " no existe"));
        if (franja.estaOcupada() || franja.yaComenzo(ahora)) {
            throw new FranjaNoDisponibleException("La franja " + franjaId + " no esta disponible");
        }
    }

    private String titulo(Long publicacionId) {
        return publicacionRepository.buscarPorId(publicacionId).map(Publicacion::getTitulo).orElse(null);
    }

    private Contratacion cargarComoParticipante(Long actorId, Long contratacionId) {
        Entradas.exigir(actorId, "El actor");
        Contratacion contratacion = Cargas.contratacion(contratacionRepository, contratacionId);
        if (!contratacion.esParticipante(actorId)) {
            throw new NoAutorizadoException("Solo los participantes de la contratacion pueden acceder a ella");
        }
        return contratacion;
    }

    private Contratacion cargarComoProveedor(Long actorId, Long contratacionId) {
        Entradas.exigir(actorId, "El actor");
        Contratacion contratacion = Cargas.contratacion(contratacionRepository, contratacionId);
        if (!contratacion.getProveedorId().equals(actorId)) {
            throw new NoAutorizadoException("Solo el proveedor puede realizar esta accion");
        }
        return contratacion;
    }

    private Contratacion cargarComoCliente(Long actorId, Long contratacionId) {
        Entradas.exigir(actorId, "El actor");
        Contratacion contratacion = Cargas.contratacion(contratacionRepository, contratacionId);
        if (!contratacion.getClienteId().equals(actorId)) {
            throw new NoAutorizadoException("Solo el cliente puede realizar esta accion");
        }
        return contratacion;
    }
}
