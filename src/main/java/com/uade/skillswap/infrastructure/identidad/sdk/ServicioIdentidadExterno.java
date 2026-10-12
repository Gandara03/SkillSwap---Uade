package com.uade.skillswap.infrastructure.identidad.sdk;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mock del SDK del proveedor de identidad. Su API es ajena a nuestro puerto (Adaptee del patron Adapter):
 * habla de uid, tokens de sesion y devuelve {@link UsuarioExterno}. Es el unico que conoce ese vocabulario.
 */
public class ServicioIdentidadExterno {

    private final Map<String, UsuarioExterno> usuarios = new LinkedHashMap<>();
    private final Map<String, String> sesiones = new LinkedHashMap<>();

    /** Da de alta (o reemplaza) una cuenta en el proveedor. */
    public void registrarCuenta(UsuarioExterno usuario) {
        usuarios.put(usuario.uid(), usuario);
    }

    /** Devuelve la cuenta o null si el proveedor no la conoce (convencion del SDK). */
    public UsuarioExterno obtenerPorId(String uid) {
        return uid == null ? null : usuarios.get(uid);
    }

    public String emailDe(String uid) {
        UsuarioExterno u = obtenerPorId(uid);
        return u == null ? null : u.correoElectronico();
    }

    public void suspenderCuenta(String uid) {
        UsuarioExterno u = obtenerPorId(uid);
        if (u != null) {
            usuarios.put(uid, new UsuarioExterno(u.uid(), u.displayName(), u.correoElectronico(),
                    UsuarioExterno.CUENTA_SUSPENDIDA));
        }
    }

    /** Abre una sesion y devuelve el token; falla si la cuenta no existe. */
    public String abrirSesion(String uid) {
        if (obtenerPorId(uid) == null) {
            throw new IllegalArgumentException("El proveedor de identidad no conoce la cuenta " + uid);
        }
        String token = "tok-" + UUID.randomUUID();
        sesiones.put(token, uid);
        return token;
    }

    public void cerrarSesion(String token) {
        sesiones.remove(token);
    }

    /** uid asociado al token, o null si el token no es valido. */
    public String uidDeSesion(String token) {
        return token == null ? null : sesiones.get(token);
    }
}
