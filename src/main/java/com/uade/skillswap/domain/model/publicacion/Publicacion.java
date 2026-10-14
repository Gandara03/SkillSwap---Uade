package com.uade.skillswap.domain.model.publicacion;

import com.uade.skillswap.domain.exception.CupoAgotadoException;
import com.uade.skillswap.domain.exception.FranjaNoDisponibleException;
import com.uade.skillswap.domain.exception.ReglaDeNegocioException;
import com.uade.skillswap.domain.model.historial.ContextoTransicion;
import com.uade.skillswap.domain.model.historial.HistorialEstado;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.politica.TipoPoliticaCancelacion;
import com.uade.skillswap.domain.model.publicacion.estado.EstadoBorrador;
import com.uade.skillswap.domain.model.publicacion.estado.EstadoPublicacion;
import com.uade.skillswap.domain.model.publicacion.estado.EstadoPublicacionFactory;
import com.uade.skillswap.domain.model.vo.Habilidad;
import com.uade.skillswap.domain.model.vo.SkillCoins;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Agregado Publicacion (mentoria, servicio o desafio). Es el Information Expert de sus reglas:
 * estados (patron State), franjas, cupo y validaciones de publicacion/solicitud.
 * El cupo se controla con optimistic locking (version) para evitar sobreventa.
 */
public class Publicacion {

    private Long id;
    private final TipoPublicacion tipo;
    private final Long autorId;
    private String titulo;
    private String descripcion;
    private String categoria;
    private Modalidad modalidad;
    private int duracionMinutos;
    private int cupo;
    private int cupoOcupado;
    private SkillCoins precioBase;
    private TipoPoliticaCancelacion politicaCancelacion = TipoPoliticaCancelacion.FLEXIBLE;
    private final Set<String> etiquetas = new LinkedHashSet<>();
    private final Set<Habilidad> habilidades = new LinkedHashSet<>();
    private final List<Franja> franjas = new ArrayList<>();
    private String estado;
    private EstadoPublicacion estadoActual;
    private final List<HistorialEstado> historial = new ArrayList<>();
    private final LocalDateTime creadaEn;
    private LocalDateTime publicadaEn;
    private Long version;

    private Publicacion(TipoPublicacion tipo, Long autorId, LocalDateTime creadaEn) {
        this.tipo = tipo;
        this.autorId = autorId;
        this.creadaEn = creadaEn;
    }

    /** Crea un borrador. Puede estar incompleto: la completitud se exige recien al publicar. */
    public static Publicacion crearBorrador(TipoPublicacion tipo, Long autorId, TipoPerfil tipoAutor,
                                            DatosPublicacion datos, LocalDateTime ahora) {
        if (tipo == null || autorId == null || tipoAutor == null) {
            throw new ReglaDeNegocioException("Tipo de publicacion, autor y tipo de autor son obligatorios");
        }
        if (tipo == TipoPublicacion.DESAFIO && tipoAutor != TipoPerfil.ORGANIZACION) {
            throw new ReglaDeNegocioException("Solo una organizacion puede publicar un desafio");
        }
        if (tipo != TipoPublicacion.DESAFIO && tipoAutor != TipoPerfil.PROFESIONAL) {
            throw new ReglaDeNegocioException("Solo un profesional puede publicar mentorias o servicios");
        }
        Publicacion p = new Publicacion(tipo, autorId, ahora);
        p.cargarDatos(datos);
        p.estadoActual = new EstadoBorrador();
        p.estado = p.estadoActual.getNombre();
        p.historial.add(HistorialEstado.registrar(null, p.estado,
                new ContextoTransicion(autorId, "Borrador creado", ahora)));
        return p;
    }

    /** Solo para rehidratar desde persistencia. */
    public static Publicacion reconstruir(Long id, TipoPublicacion tipo, Long autorId, String titulo, String descripcion,
                                          String categoria, Modalidad modalidad, int duracionMinutos, int cupo,
                                          int cupoOcupado, SkillCoins precioBase, TipoPoliticaCancelacion politica,
                                          Collection<String> etiquetas, Collection<Habilidad> habilidades,
                                          Collection<Franja> franjas, String estado,
                                          Collection<HistorialEstado> historial, LocalDateTime creadaEn,
                                          LocalDateTime publicadaEn, Long version) {
        Publicacion p = new Publicacion(tipo, autorId, creadaEn);
        p.id = id;
        p.titulo = titulo;
        p.descripcion = descripcion;
        p.categoria = categoria;
        p.modalidad = modalidad;
        p.duracionMinutos = duracionMinutos;
        p.cupo = cupo;
        p.cupoOcupado = cupoOcupado;
        p.precioBase = precioBase;
        p.politicaCancelacion = politica;
        p.etiquetas.addAll(etiquetas);
        p.habilidades.addAll(habilidades);
        p.franjas.addAll(franjas);
        p.estado = estado;
        p.estadoActual = EstadoPublicacionFactory.desdeNombre(estado);
        p.historial.addAll(historial);
        p.publicadaEn = publicadaEn;
        p.version = version;
        return p;
    }

    // ---------- edicion ----------

    /** Solo editable en BORRADOR. */
    public void editar(DatosPublicacion datos) {
        if (!estadoActual.permiteEdicion()) {
            throw new ReglaDeNegocioException("Solo se puede editar una publicacion en BORRADOR (estado actual: "
                    + estado + ")");
        }
        cargarDatos(datos);
    }

    /** Copia los datos editables. Etiquetas y habilidades se reemplazan; la politica solo cambia si viene. */
    private void cargarDatos(DatosPublicacion datos) {
        if (datos == null) {
            throw new ReglaDeNegocioException("Los datos de la publicacion son obligatorios");
        }
        this.titulo = datos.titulo();
        this.descripcion = datos.descripcion();
        this.categoria = datos.categoria();
        this.modalidad = datos.modalidad();
        this.duracionMinutos = datos.duracionMinutos();
        this.cupo = datos.cupo();
        this.precioBase = datos.precioBase();
        if (datos.politicaCancelacion() != null) {
            this.politicaCancelacion = datos.politicaCancelacion();
        }
        this.etiquetas.clear();
        if (datos.etiquetas() != null) {
            this.etiquetas.addAll(datos.etiquetas());
        }
        this.habilidades.clear();
        if (datos.habilidades() != null) {
            this.habilidades.addAll(datos.habilidades());
        }
    }

    /** Agrega una franja futura de disponibilidad (no permitido en estados terminales). */
    public Franja agregarFranja(LocalDateTime inicio, LocalDateTime fin, LocalDateTime ahora) {
        if (estadoActual.esTerminal()) {
            throw new ReglaDeNegocioException("No se pueden agregar franjas a una publicacion " + estado);
        }
        if (inicio != null && !inicio.isAfter(ahora)) {
            throw new ReglaDeNegocioException("La franja debe comenzar en el futuro");
        }
        Franja franja = Franja.nueva(inicio, fin);
        franjas.add(franja);
        return franja;
    }

    // ---------- transiciones (patron State) ----------

    public void publicar(Long actorId, LocalDateTime ahora) {
        estadoActual.publicar(this, new ContextoTransicion(actorId, "Publicacion publicada", ahora));
    }

    public void pausar(Long actorId, LocalDateTime ahora) {
        estadoActual.pausar(this, new ContextoTransicion(actorId, "Publicacion pausada", ahora));
    }

    public void reanudar(Long actorId, LocalDateTime ahora) {
        estadoActual.reanudar(this, new ContextoTransicion(actorId, "Publicacion reanudada", ahora));
    }

    public void cerrar(Long actorId, LocalDateTime ahora) {
        estadoActual.cerrar(this, new ContextoTransicion(actorId, "Publicacion cerrada", ahora));
    }

    public void cancelar(Long actorId, String motivo, LocalDateTime ahora) {
        estadoActual.cancelar(this, new ContextoTransicion(actorId, motivo, ahora));
    }

    /** Lo usan los estados concretos: cambia el estado y registra el historial inmutable. */
    public void cambiarEstado(EstadoPublicacion nuevo, ContextoTransicion t) {
        String anterior = this.estado;
        this.estadoActual = nuevo;
        this.estado = nuevo.getNombre();
        this.historial.add(HistorialEstado.registrar(anterior, this.estado, t));
    }

    public void marcarPublicada(LocalDateTime fecha) {
        if (publicadaEn == null) {
            this.publicadaEn = fecha;
        }
    }

    /** Publicar requiere titulo, categoria, precio no negativo y al menos una franja de disponibilidad. */
    public void validarCompletaParaPublicar() {
        List<String> faltantes = new ArrayList<>();
        if (titulo == null || titulo.isBlank()) {
            faltantes.add("titulo");
        }
        if (categoria == null || categoria.isBlank()) {
            faltantes.add("categoria");
        }
        if (precioBase == null || precioBase.esNegativo()) {
            faltantes.add("precio no negativo");
        }
        if (franjas.isEmpty()) {
            faltantes.add("al menos una franja de disponibilidad");
        }
        if (cupo < 1) {
            faltantes.add("cupo mayor a cero");
        }
        if (duracionMinutos < 1) {
            faltantes.add("duracion estimada");
        }
        if (modalidad == null) {
            faltantes.add("modalidad");
        }
        if (!faltantes.isEmpty()) {
            throw new ReglaDeNegocioException("La publicacion esta incompleta, falta: " + String.join(", ", faltantes));
        }
    }

    // ---------- solicitud y reserva ----------

    /** Valida si un cliente puede solicitar esta publicacion en la franja indicada. */
    public void validarSolicitud(Long clienteId, Long franjaId, LocalDateTime ahora) {
        if (!estadoActual.admiteSolicitudes()) {
            throw new ReglaDeNegocioException("La publicacion no admite solicitudes (estado " + estado + ")");
        }
        if (autorId.equals(clienteId)) {
            throw new ReglaDeNegocioException("El autor no puede solicitar su propia publicacion");
        }
        exigirFranjaLibre(franjaId, ahora);
        exigirCupo();
    }

    /** Reserva la franja y consume un cupo. Falla si la franja esta ocupada o no queda cupo. */
    public void reservarFranja(Long franjaId, LocalDateTime ahora) {
        if (estadoActual.esTerminal()) {
            throw new ReglaDeNegocioException("La publicacion esta " + estado + " y no admite reservas");
        }
        Franja franja = exigirFranjaLibre(franjaId, ahora);
        exigirCupo();
        franja.ocupar();
        cupoOcupado++;
    }

    /** Libera la franja y devuelve el cupo (cancelacion de una reserva). */
    public void liberarFranja(Long franjaId) {
        Franja franja = buscarFranja(franjaId)
                .orElseThrow(() -> new FranjaNoDisponibleException("La franja " + franjaId + " no existe"));
        if (franja.estaOcupada()) {
            franja.liberar();
            cupoOcupado = Math.max(0, cupoOcupado - 1);
        }
    }

    /** Mueve una reserva de una franja a otra sin alterar el cupo total ocupado. */
    public void reprogramarReserva(Long franjaVieja, Long franjaNueva, LocalDateTime ahora) {
        Franja nueva = exigirFranjaLibre(franjaNueva, ahora);
        Franja vieja = buscarFranja(franjaVieja)
                .orElseThrow(() -> new FranjaNoDisponibleException("La franja " + franjaVieja + " no existe"));
        vieja.liberar();
        nueva.ocupar();
    }

    private Franja exigirFranjaLibre(Long franjaId, LocalDateTime ahora) {
        Franja franja = buscarFranja(franjaId)
                .orElseThrow(() -> new FranjaNoDisponibleException("La franja " + franjaId + " no existe"));
        if (franja.estaOcupada()) {
            throw new FranjaNoDisponibleException("La franja ya esta ocupada");
        }
        if (franja.yaComenzo(ahora)) {
            throw new FranjaNoDisponibleException("La franja ya comenzo o ya paso");
        }
        return franja;
    }

    private void exigirCupo() {
        if (cupoOcupado >= cupo) {
            throw new CupoAgotadoException("La publicacion no tiene cupo disponible");
        }
    }

    public Optional<Franja> buscarFranja(Long franjaId) {
        if (franjaId == null) {
            return Optional.empty();
        }
        return franjas.stream().filter(f -> franjaId.equals(f.getId())).findFirst();
    }

    // ---------- consultas ----------

    public boolean admiteSolicitudes() {
        return estadoActual.admiteSolicitudes();
    }

    public boolean esVisiblePara(Long perfilId) {
        return estadoActual.esVisiblePublicamente() || autorId.equals(perfilId);
    }

    public boolean estaPublicada() {
        return "PUBLICADA".equals(estado);
    }

    public boolean esAutor(Long perfilId) {
        return autorId.equals(perfilId);
    }

    public int getCupoDisponible() {
        return Math.max(0, cupo - cupoOcupado);
    }

    public Long getId() {
        return id;
    }

    public TipoPublicacion getTipo() {
        return tipo;
    }

    public Long getAutorId() {
        return autorId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public int getCupo() {
        return cupo;
    }

    public int getCupoOcupado() {
        return cupoOcupado;
    }

    public SkillCoins getPrecioBase() {
        return precioBase;
    }

    public TipoPoliticaCancelacion getPoliticaCancelacion() {
        return politicaCancelacion;
    }

    public Set<String> getEtiquetas() {
        return Collections.unmodifiableSet(etiquetas);
    }

    public Set<Habilidad> getHabilidades() {
        return Collections.unmodifiableSet(habilidades);
    }

    public List<Franja> getFranjas() {
        return Collections.unmodifiableList(franjas);
    }

    public String getEstado() {
        return estado;
    }

    public List<HistorialEstado> getHistorial() {
        return Collections.unmodifiableList(historial);
    }

    public LocalDateTime getCreadaEn() {
        return creadaEn;
    }

    public LocalDateTime getPublicadaEn() {
        return publicadaEn;
    }

    public Long getVersion() {
        return version;
    }
}
