package com.uade.skillswap.domain.model.contratacion;

import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.exception.TransicionInvalidaException;
import com.uade.skillswap.domain.model.contratacion.estado.EstadoContratacion;
import com.uade.skillswap.domain.model.contratacion.estado.EstadoContratacionFactory;
import com.uade.skillswap.domain.model.contratacion.estado.EstadoPendiente;
import com.uade.skillswap.domain.model.contratacion.observer.ContratacionEvent;
import com.uade.skillswap.domain.model.contratacion.observer.ContratacionObserver;
import com.uade.skillswap.domain.model.contratacion.observer.ContratacionSubject;
import com.uade.skillswap.domain.model.historial.ContextoTransicion;
import com.uade.skillswap.domain.model.historial.HistorialEstado;
import com.uade.skillswap.domain.model.politica.PoliticaCancelacion;
import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Agregado Contratacion: solicitud + reserva + pago + sesion. Una solicitud es una contratacion en estado
 * PENDIENTE. Usa State para las transiciones y es Subject de Observer: los eventos se acumulan y se
 * publican a los observadores recien despues de confirmar la transaccion (notificarEventosPendientes).
 */
public class Contratacion implements ContratacionSubject {

    private Long id;
    private final Long publicacionId;
    private final Long clienteId;
    private final Long proveedorId;
    private Long franjaId;
    private final String mensaje;
    private final String objetivo;
    private final SkillCoins precioFinal;
    private String estado;
    private EstadoContratacion estadoActual;
    private PropuestaReprogramacion propuestaReprogramacion;
    private final List<HistorialEstado> historial = new ArrayList<>();
    private final LocalDateTime creadaEn;
    private LocalDateTime pagadaEn;
    private LocalDateTime completadaEn;
    private Long canceladaPorId;
    private Long version;

    private final List<ContratacionObserver> observers = new ArrayList<>();
    private final List<ContratacionEvent> eventosPendientes = new ArrayList<>();

    private Contratacion(Long publicacionId, Long clienteId, Long proveedorId, Long franjaId, String mensaje,
                         String objetivo, SkillCoins precioFinal, LocalDateTime creadaEn) {
        this.publicacionId = publicacionId;
        this.clienteId = clienteId;
        this.proveedorId = proveedorId;
        this.franjaId = franjaId;
        this.mensaje = mensaje;
        this.objetivo = objetivo;
        this.precioFinal = precioFinal;
        this.creadaEn = creadaEn;
    }

    /** Crea una solicitud (estado PENDIENTE). El precio final ya incluye promociones (Decorator). */
    public static Contratacion solicitar(Long publicacionId, Long clienteId, Long proveedorId, Long franjaId,
                                         String mensaje, String objetivo, SkillCoins precioFinal,
                                         LocalDateTime ahora) {
        if (clienteId == null || proveedorId == null || publicacionId == null || franjaId == null) {
            throw new ReglaDeNegocioException("Publicacion, franja, cliente y proveedor son obligatorios");
        }
        if (clienteId.equals(proveedorId)) {
            throw new ReglaDeNegocioException("El autor no puede solicitar su propia publicacion");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new ReglaDeNegocioException("El mensaje de la solicitud es obligatorio");
        }
        if (objetivo == null || objetivo.isBlank()) {
            throw new ReglaDeNegocioException("El objetivo de la solicitud es obligatorio");
        }
        if (precioFinal == null || precioFinal.esNegativo()) {
            throw new ReglaDeNegocioException("El precio final no puede ser negativo");
        }
        Contratacion c = new Contratacion(publicacionId, clienteId, proveedorId, franjaId, mensaje.trim(),
                objetivo.trim(), precioFinal, ahora);
        c.estadoActual = new EstadoPendiente();
        c.estado = c.estadoActual.getNombre();
        ContextoTransicion t = new ContextoTransicion(clienteId, "Solicitud creada", ahora);
        c.historial.add(HistorialEstado.registrar(null, c.estado, t));
        c.registrarEvento(TipoEventoContratacion.SOLICITUD_RECIBIDA, clienteId, "Nueva solicitud", ahora);
        return c;
    }

    /** Solo para rehidratar desde persistencia. */
    public static Contratacion reconstruir(Long id, Long publicacionId, Long clienteId, Long proveedorId,
                                           Long franjaId, String mensaje, String objetivo, SkillCoins precioFinal,
                                           String estado, PropuestaReprogramacion propuesta,
                                           Collection<HistorialEstado> historial, LocalDateTime creadaEn,
                                           LocalDateTime pagadaEn, LocalDateTime completadaEn, Long canceladaPorId,
                                           Long version) {
        Contratacion c = new Contratacion(publicacionId, clienteId, proveedorId, franjaId, mensaje, objetivo,
                precioFinal, creadaEn);
        c.id = id;
        c.estado = estado;
        c.estadoActual = EstadoContratacionFactory.desdeNombre(estado);
        c.propuestaReprogramacion = propuesta;
        c.historial.addAll(historial);
        c.pagadaEn = pagadaEn;
        c.completadaEn = completadaEn;
        c.canceladaPorId = canceladaPorId;
        c.version = version;
        return c;
    }

    // ---------- acciones (delegan en el State) ----------

    public void aceptar(Long actorId, LocalDateTime ahora) {
        exigirProveedor(actorId);
        estadoActual.aceptar(this, new ContextoTransicion(actorId, "Solicitud aceptada por el proveedor", ahora));
        registrarEvento(TipoEventoContratacion.SOLICITUD_ACEPTADA, actorId, "Solicitud aceptada", ahora);
    }

    public void rechazar(Long actorId, String motivo, LocalDateTime ahora) {
        exigirProveedor(actorId);
        estadoActual.rechazar(this, new ContextoTransicion(actorId, motivo, ahora));
        registrarEvento(TipoEventoContratacion.SOLICITUD_RECHAZADA, actorId, motivo, ahora);
    }

    public void pagar(Long actorId, LocalDateTime ahora) {
        exigirCliente(actorId);
        estadoActual.pagar(this, new ContextoTransicion(actorId, "Pago confirmado con SkillCoins", ahora));
        this.pagadaEn = ahora;
        registrarEvento(TipoEventoContratacion.PAGO_CONFIRMADO, actorId, "Pago de " + precioFinal, ahora);
    }

    public void iniciar(Long actorId, LocalDateTime ahora) {
        exigirProveedor(actorId);
        estadoActual.iniciar(this, new ContextoTransicion(actorId, "Sesion iniciada", ahora));
    }

    public void completar(Long actorId, LocalDateTime ahora) {
        exigirProveedor(actorId);
        estadoActual.completar(this, new ContextoTransicion(actorId, "Sesion completada", ahora));
        this.completadaEn = ahora;
        registrarEvento(TipoEventoContratacion.SESION_COMPLETADA, actorId, "Sesion completada", ahora);
    }

    public void cancelar(Long actorId, String motivo, LocalDateTime ahora) {
        exigirParticipante(actorId);
        estadoActual.cancelar(this, new ContextoTransicion(actorId, motivo, ahora));
        this.canceladaPorId = actorId;
        this.propuestaReprogramacion = null;
        registrarEvento(TipoEventoContratacion.CANCELACION, actorId, motivo, ahora);
    }

    /**
     * Reparte lo retenido (escrow) de una contratacion cancelada. Si no se habia pagado, no hay nada que repartir.
     * Si cancelo el proveedor, el cliente recupera el 100 %: la politica protege al proveedor de las cancelaciones
     * tardias del cliente, no lo beneficia cuando cancela el. Si cancelo el cliente, el reintegro lo decide la
     * politica de la publicacion (Strategy) segun la anticipacion. Lo que no se reintegra compensa al proveedor.
     */
    public RepartoCancelacion repartirRetenido(PoliticaCancelacion politica, Duration anticipacion) {
        if (canceladaPorId == null) {
            throw new ReglaDeNegocioException("Solo se reparte lo retenido de una contratacion cancelada");
        }
        if (!estaPagada()) {
            return new RepartoCancelacion(SkillCoins.CERO, SkillCoins.CERO);
        }
        SkillCoins reintegro = proveedorId.equals(canceladaPorId)
                ? precioFinal
                : politica.calcularReintegro(precioFinal, anticipacion);
        return new RepartoCancelacion(reintegro, precioFinal.restar(reintegro));
    }

    /** Cualquiera de las partes propone mover la reserva a otra franja (antes de comenzar). */
    public void proponerReprogramacion(Long actorId, Long nuevaFranjaId, LocalDateTime ahora) {
        exigirParticipante(actorId);
        if (!estadoActual.permiteReprogramacion()) {
            throw new TransicionInvalidaException("contratacion", estado, "reprogramar");
        }
        if (nuevaFranjaId == null || nuevaFranjaId.equals(franjaId)) {
            throw new ReglaDeNegocioException("La nueva franja debe ser distinta a la actual");
        }
        if (propuestaReprogramacion != null) {
            throw new ReglaDeNegocioException("Ya hay una propuesta de reprogramacion pendiente");
        }
        this.propuestaReprogramacion = new PropuestaReprogramacion(nuevaFranjaId, actorId, ahora);
        registrarNota("Reprogramacion propuesta", actorId, ahora);
        registrarEvento(TipoEventoContratacion.REPROGRAMACION_PROPUESTA, actorId,
                "Propuesta de reprogramacion a la franja " + nuevaFranjaId, ahora);
    }

    /** La contraparte de quien propuso acepta o rechaza. Si acepta, la reserva pasa a la nueva franja. */
    public void responderReprogramacion(Long actorId, boolean acepta, LocalDateTime ahora) {
        exigirParticipante(actorId);
        if (propuestaReprogramacion == null) {
            throw new ReglaDeNegocioException("No hay una propuesta de reprogramacion pendiente");
        }
        if (!estadoActual.permiteReprogramacion()) {
            throw new TransicionInvalidaException("contratacion", estado, "reprogramar");
        }
        if (propuestaReprogramacion.getPropuestaPorId().equals(actorId)) {
            throw new ReglaDeNegocioException("La propuesta debe ser respondida por la otra parte");
        }
        if (acepta) {
            this.franjaId = propuestaReprogramacion.getNuevaFranjaId();
            registrarNota("Reprogramacion aceptada", actorId, ahora);
            registrarEvento(TipoEventoContratacion.REPROGRAMACION_ACEPTADA, actorId,
                    "Reserva reprogramada a la franja " + franjaId, ahora);
        } else {
            registrarNota("Reprogramacion rechazada", actorId, ahora);
            registrarEvento(TipoEventoContratacion.REPROGRAMACION_RECHAZADA, actorId,
                    "Reprogramacion rechazada", ahora);
        }
        this.propuestaReprogramacion = null;
    }

    public Comprobante consultarComprobante() {
        if (!estaCompletada()) {
            throw new ReglaDeNegocioException("El comprobante solo esta disponible para contrataciones COMPLETADAS");
        }
        return new Comprobante("SS-" + String.format("%08d", id == null ? 0 : id), id, publicacionId, clienteId,
                proveedorId, precioFinal, pagadaEn, completadaEn);
    }

    /** Lo usan los estados concretos: cambia el estado y registra el historial inmutable. */
    public void cambiarEstado(EstadoContratacion nuevo, ContextoTransicion t) {
        String anterior = this.estado;
        this.estadoActual = nuevo;
        this.estado = nuevo.getNombre();
        this.historial.add(HistorialEstado.registrar(anterior, this.estado, t));
    }

    private void registrarNota(String nota, Long actorId, LocalDateTime ahora) {
        historial.add(HistorialEstado.registrar(estado, estado, new ContextoTransicion(actorId, nota, ahora)));
    }

    // ---------- reglas de participacion ----------

    private void exigirProveedor(Long actorId) {
        if (!proveedorId.equals(actorId)) {
            throw new ReglaDeNegocioException("Solo el proveedor puede realizar esta accion");
        }
    }

    private void exigirCliente(Long actorId) {
        if (!clienteId.equals(actorId)) {
            throw new ReglaDeNegocioException("Solo el cliente puede realizar esta accion");
        }
    }

    private void exigirParticipante(Long actorId) {
        if (!esParticipante(actorId)) {
            throw new ReglaDeNegocioException("Solo los participantes de la contratacion pueden realizar esta accion");
        }
    }

    public boolean esParticipante(Long perfilId) {
        return clienteId.equals(perfilId) || proveedorId.equals(perfilId);
    }

    public Long contraparteDe(Long perfilId) {
        if (clienteId.equals(perfilId)) {
            return proveedorId;
        }
        if (proveedorId.equals(perfilId)) {
            return clienteId;
        }
        throw new ReglaDeNegocioException("El perfil " + perfilId + " no participa de la contratacion");
    }

    // ---------- Observer ----------

    private void registrarEvento(TipoEventoContratacion tipo, Long actorId, String detalle, LocalDateTime fecha) {
        eventosPendientes.add(new ContratacionEvent(tipo, id, publicacionId, clienteId, proveedorId, actorId,
                detalle, fecha));
    }

    @Override
    public void agregarObserver(ContratacionObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    @Override
    public void quitarObserver(ContratacionObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notificarObservers(ContratacionEvent evento) {
        for (ContratacionObserver observer : observers) {
            observer.actualizar(evento);
        }
    }

    public List<ContratacionEvent> eventosPendientes() {
        return Collections.unmodifiableList(eventosPendientes);
    }

    /**
     * Publica a los observadores los eventos acumulados. Se llama despues de confirmar la transaccion,
     * para no notificar operaciones que luego se revierten. El evento se reconstruye con el id definitivo.
     */
    public void notificarEventosPendientes() {
        List<ContratacionEvent> pendientes = new ArrayList<>(eventosPendientes);
        eventosPendientes.clear();
        for (ContratacionEvent e : pendientes) {
            notificarObservers(new ContratacionEvent(e.getTipo(), id, e.getPublicacionId(), e.getClienteId(),
                    e.getProveedorId(), e.getActorId(), e.getDetalle(), e.getFecha()));
        }
    }

    // ---------- consultas ----------

    public boolean estaCompletada() {
        return "COMPLETADA".equals(estado);
    }

    public boolean estaPagada() {
        return pagadaEn != null;
    }

    public Long getId() {
        return id;
    }

    public Long getPublicacionId() {
        return publicacionId;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Long getProveedorId() {
        return proveedorId;
    }

    public Long getFranjaId() {
        return franjaId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public SkillCoins getPrecioFinal() {
        return precioFinal;
    }

    public String getEstado() {
        return estado;
    }

    public PropuestaReprogramacion getPropuestaReprogramacion() {
        return propuestaReprogramacion;
    }

    public List<HistorialEstado> getHistorial() {
        return Collections.unmodifiableList(historial);
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public LocalDateTime getPagadaEn() {
        return pagadaEn;
    }

    public LocalDateTime getCompletadaEn() {
        return completadaEn;
    }

    public Long getCanceladaPorId() {
        return canceladaPorId;
    }

    public Long getVersion() {
        return version;
    }
}
