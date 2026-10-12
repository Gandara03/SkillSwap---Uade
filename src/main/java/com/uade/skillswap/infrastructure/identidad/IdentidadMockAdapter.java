package com.uade.skillswap.infrastructure.identidad;

import com.uade.skillswap.application.port.out.IdentidadPort;
import com.uade.skillswap.infrastructure.identidad.sdk.ServicioIdentidadExterno;
import com.uade.skillswap.infrastructure.identidad.sdk.UsuarioExterno;

import java.util.Optional;

/**
 * Adapter: traduce el SDK del proveedor (uid, tokens, estadoCuenta) a {@link IdentidadPort}.
 * Mantiene el token de la sesion actual; la aplicacion solo ve {@code UsuarioIdentidad}.
 */
public class IdentidadMockAdapter implements IdentidadPort {

    private final ServicioIdentidadExterno servicio;
    private String tokenActual;

    /** Usa un proveedor ya armado (por ejemplo, uno con cuentas propias del test). */
    public IdentidadMockAdapter(ServicioIdentidadExterno servicio) {
        this.servicio = servicio;
    }

    /** Crea el proveedor con las cuentas de la demo ya precargadas. */
    public static IdentidadMockAdapter conUsuariosDemo() {
        ServicioIdentidadExterno servicio = new ServicioIdentidadExterno();
        precargarUsuariosDemo(servicio);
        return new IdentidadMockAdapter(servicio);
    }

    /** Precarga u-ana, u-bruno, u-cecilia y u-techcorp en el proveedor. */
    public static void precargarUsuariosDemo(ServicioIdentidadExterno servicio) {
        servicio.registrarCuenta(cuenta("u-ana", "Ana Gomez", "ana@skillswap.test"));
        servicio.registrarCuenta(cuenta("u-bruno", "Bruno Diaz", "bruno@skillswap.test"));
        servicio.registrarCuenta(cuenta("u-cecilia", "Cecilia Paz", "cecilia@skillswap.test"));
        servicio.registrarCuenta(cuenta("u-techcorp", "TechCorp S.A.", "contacto@techcorp.test"));
    }

    private static UsuarioExterno cuenta(String uid, String nombre, String email) {
        return new UsuarioExterno(uid, nombre, email, UsuarioExterno.CUENTA_ACTIVA);
    }

    /** Inicia sesion como el usuario indicado; falla si el proveedor no lo conoce. */
    public void iniciarSesion(String userId) {
        cerrarSesion();
        this.tokenActual = servicio.abrirSesion(userId);
    }

    public void cerrarSesion() {
        if (tokenActual != null) {
            servicio.cerrarSesion(tokenActual);
            tokenActual = null;
        }
    }

    @Override
    public Optional<UsuarioIdentidad> usuarioActual() {
        return Optional.ofNullable(servicio.uidDeSesion(tokenActual)).flatMap(this::buscarPorUserId);
    }

    @Override
    public Optional<UsuarioIdentidad> buscarPorUserId(String userId) {
        return Optional.ofNullable(servicio.obtenerPorId(userId)).map(IdentidadMockAdapter::traducir);
    }

    private static UsuarioIdentidad traducir(UsuarioExterno u) {
        return new UsuarioIdentidad(u.uid(), u.displayName(), u.correoElectronico(),
                UsuarioExterno.CUENTA_ACTIVA.equals(u.estadoCuenta()));
    }
}
