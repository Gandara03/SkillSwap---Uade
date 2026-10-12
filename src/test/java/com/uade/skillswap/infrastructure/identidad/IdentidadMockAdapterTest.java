package com.uade.skillswap.infrastructure.identidad;

import com.uade.skillswap.application.port.out.IdentidadPort;
import com.uade.skillswap.infrastructure.identidad.sdk.ServicioIdentidadExterno;
import com.uade.skillswap.infrastructure.identidad.sdk.UsuarioExterno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IdentidadMockAdapter (Adapter sobre el SDK externo)")
class IdentidadMockAdapterTest {

    private ServicioIdentidadExterno servicio;
    private IdentidadMockAdapter adapter;

    @BeforeEach
    void preparar() {
        servicio = new ServicioIdentidadExterno();
        IdentidadMockAdapter.precargarUsuariosDemo(servicio);
        adapter = new IdentidadMockAdapter(servicio);
    }

    @Test
    @DisplayName("sin sesion iniciada no hay usuario actual")
    void sinSesion() {
        assertTrue(adapter.usuarioActual().isEmpty());
    }

    @Test
    @DisplayName("iniciarSesion expone al usuario traducido al modelo del puerto")
    void iniciarSesion() {
        adapter.iniciarSesion("u-ana");
        IdentidadPort.UsuarioIdentidad u = adapter.usuarioActual().orElseThrow();
        assertEquals("u-ana", u.userId());
        assertEquals("Ana Gomez", u.nombre());
        assertEquals("ana@skillswap.test", u.email());
        assertTrue(u.activo());
    }

    @Test
    @DisplayName("cerrarSesion deja sin usuario actual y es idempotente")
    void cerrarSesion() {
        adapter.iniciarSesion("u-bruno");
        adapter.cerrarSesion();
        adapter.cerrarSesion();
        assertTrue(adapter.usuarioActual().isEmpty());
    }

    @Test
    @DisplayName("iniciar sesion con otro usuario reemplaza al anterior y cierra su token")
    void cambioDeUsuario() {
        adapter.iniciarSesion("u-ana");
        adapter.iniciarSesion("u-bruno");
        assertEquals("u-bruno", adapter.usuarioActual().orElseThrow().userId());
    }

    @Test
    @DisplayName("un usuario desconocido no puede iniciar sesion")
    void usuarioDesconocido() {
        assertThrows(IllegalArgumentException.class, () -> adapter.iniciarSesion("u-nadie"));
        assertTrue(adapter.usuarioActual().isEmpty());
    }

    @Test
    @DisplayName("buscarPorUserId traduce usuarios existentes y devuelve vacio para los inexistentes")
    void buscarPorUserId() {
        assertEquals("Cecilia Paz", adapter.buscarPorUserId("u-cecilia").orElseThrow().nombre());
        assertTrue(adapter.buscarPorUserId("u-nadie").isEmpty());
        assertTrue(adapter.buscarPorUserId(null).isEmpty());
    }

    @Test
    @DisplayName("precarga las cuatro cuentas de la demo")
    void precarga() {
        for (String id : new String[]{"u-ana", "u-bruno", "u-cecilia", "u-techcorp"}) {
            assertTrue(adapter.buscarPorUserId(id).isPresent(), id);
        }
        assertTrue(IdentidadMockAdapter.conUsuariosDemo().buscarPorUserId("u-techcorp").isPresent());
    }

    @Test
    @DisplayName("el estado de cuenta del proveedor (texto) se traduce a booleano activo")
    void cuentaSuspendida() {
        servicio.suspenderCuenta("u-bruno");
        assertFalse(adapter.buscarPorUserId("u-bruno").orElseThrow().activo());
        assertTrue(adapter.buscarPorUserId("u-ana").orElseThrow().activo());
    }

    @Test
    @DisplayName("el SDK externo tiene API propia: uid, email y tokens de sesion")
    void apiDelSdk() {
        assertEquals("ana@skillswap.test", servicio.emailDe("u-ana"));
        assertNull(servicio.emailDe("u-nadie"));
        assertNull(servicio.obtenerPorId("u-nadie"));
        String token = servicio.abrirSesion("u-ana");
        assertEquals("u-ana", servicio.uidDeSesion(token));
        servicio.cerrarSesion(token);
        assertNull(servicio.uidDeSesion(token));
        assertEquals(UsuarioExterno.CUENTA_ACTIVA, servicio.obtenerPorId("u-ana").estadoCuenta());
    }
}
