package com.uade.skillswap.infrastructure.notificacion;

import com.uade.skillswap.application.port.out.CanalNotificacionPort;
import com.uade.skillswap.domain.model.contratacion.TipoEventoContratacion;
import com.uade.skillswap.domain.model.notificacion.CanalNotificacion;
import com.uade.skillswap.domain.model.notificacion.Notificacion;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.infrastructure.identidad.IdentidadMockAdapter;
import com.uade.skillswap.infrastructure.notificacion.sdk.EmailSdkMock;
import com.uade.skillswap.infrastructure.notificacion.sdk.PushSdkMock;
import com.uade.skillswap.support.Datos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Adapters de canales de notificacion y Simple Factory")
class CanalesNotificacionTest {

    private EmailSdkMock emailSdk;
    private PushSdkMock pushSdk;
    private EmailCanalAdapter email;
    private PushCanalAdapter push;
    private InternaCanalAdapter interna;
    private CanalNotificacionFactoryImpl factory;
    private Perfil destinatario;

    @BeforeEach
    void preparar() {
        emailSdk = new EmailSdkMock();
        pushSdk = new PushSdkMock();
        email = new EmailCanalAdapter(emailSdk, IdentidadMockAdapter.conUsuariosDemo());
        push = new PushCanalAdapter(pushSdk);
        interna = new InternaCanalAdapter();
        factory = new CanalNotificacionFactoryImpl(emailSdk, pushSdk, IdentidadMockAdapter.conUsuariosDemo());
        destinatario = Datos.profesional(1L, "u-ana");
    }

    private Notificacion notificacion(CanalNotificacion canal) {
        return Notificacion.crear(1L, TipoEventoContratacion.SOLICITUD_RECIBIDA, "Tenes una nueva solicitud", canal,
                Datos.AHORA);
    }

    @Test
    @DisplayName("EmailCanalAdapter traduce la notificacion a una llamada del SDK de correo")
    void emailEnvia() {
        email.enviar(notificacion(CanalNotificacion.EMAIL), destinatario);
        assertEquals(1, emailSdk.enviados().size());
        EmailSdkMock.Correo correo = emailSdk.enviados().get(0);
        assertEquals("ana@skillswap.test", correo.destino(), "el email real del usuario segun la identidad");
        assertTrue(correo.asunto().contains("SOLICITUD_RECIBIDA"));
        assertEquals("Tenes una nueva solicitud", correo.cuerpo());
        assertEquals(CanalNotificacion.EMAIL, email.getCanal());
    }

    @Test
    @DisplayName("EmailCanalAdapter arma la direccion con el userId si la identidad no conoce al usuario")
    void emailSinUsuarioEnIdentidad() {
        Perfil desconocido = Datos.profesional(9L, "u-desconocido");
        email.enviar(notificacion(CanalNotificacion.EMAIL), desconocido);
        assertEquals("u-desconocido@skillswap.local", emailSdk.enviados().get(0).destino());
    }

    @Test
    @DisplayName("PushCanalAdapter traduce la notificacion a una llamada del SDK de push")
    void pushEnvia() {
        push.enviar(notificacion(CanalNotificacion.PUSH), destinatario);
        assertEquals(1, pushSdk.enviados().size());
        PushSdkMock.Push p = pushSdk.enviados().get(0);
        assertEquals("dispositivo-u-ana", p.dispositivo());
        assertEquals("Tenes una nueva solicitud", p.texto());
        assertEquals(CanalNotificacion.PUSH, push.getCanal());
    }

    @Test
    @DisplayName("InternaCanalAdapter no usa ningun sistema externo")
    void internaNoLlamaAfuera() {
        interna.enviar(notificacion(CanalNotificacion.INTERNA), destinatario);
        assertEquals(CanalNotificacion.INTERNA, interna.getCanal());
        assertTrue(emailSdk.enviados().isEmpty());
        assertTrue(pushSdk.enviados().isEmpty());
    }

    @Test
    @DisplayName("la fabrica crea el adapter del canal pedido")
    void fabricaPorCanal() {
        assertInstanceOf(EmailCanalAdapter.class, factory.crear(CanalNotificacion.EMAIL));
        assertInstanceOf(PushCanalAdapter.class, factory.crear(CanalNotificacion.PUSH));
        assertInstanceOf(InternaCanalAdapter.class, factory.crear(CanalNotificacion.INTERNA));
    }

    @Test
    @DisplayName("la fabrica crea un adapter nuevo en cada pedido")
    void fabricaCreaEnCadaPedido() {
        for (CanalNotificacion canal : CanalNotificacion.values()) {
            assertNotSame(factory.crear(canal), factory.crear(canal));
        }
    }

    @Test
    @DisplayName("los adapters que crea la fabrica envian por el SDK de su proveedor")
    void fabricaUsaLosSdk() {
        factory.crear(CanalNotificacion.EMAIL).enviar(notificacion(CanalNotificacion.EMAIL), destinatario);
        factory.crear(CanalNotificacion.PUSH).enviar(notificacion(CanalNotificacion.PUSH), destinatario);
        assertEquals("ana@skillswap.test", emailSdk.enviados().get(0).destino());
        assertEquals("dispositivo-u-ana", pushSdk.enviados().get(0).dispositivo());
    }

    @Test
    @DisplayName("cada adapter devuelto reporta el canal para el que fue pedido")
    void fabricaCoherente() {
        for (CanalNotificacion canal : CanalNotificacion.values()) {
            CanalNotificacionPort puerto = factory.crear(canal);
            assertEquals(canal, puerto.getCanal());
        }
    }

    @Test
    @DisplayName("canal nulo cae en el canal interno")
    void fabricaCanalNulo() {
        assertInstanceOf(InternaCanalAdapter.class, factory.crear(null));
    }

    @Test
    @DisplayName("la fabrica exige los dos SDK y la identidad")
    void fabricaExigeDependencias() {
        IdentidadMockAdapter identidad = IdentidadMockAdapter.conUsuariosDemo();
        assertThrows(NullPointerException.class, () -> new CanalNotificacionFactoryImpl(null, pushSdk, identidad));
        assertThrows(NullPointerException.class, () -> new CanalNotificacionFactoryImpl(emailSdk, null, identidad));
        assertThrows(NullPointerException.class, () -> new CanalNotificacionFactoryImpl(emailSdk, pushSdk, null));
    }

    @Test
    @DisplayName("las listas de enviados de los SDK son de solo lectura")
    void listasInmutables() {
        assertThrows(UnsupportedOperationException.class, () -> emailSdk.enviados().clear());
        assertThrows(UnsupportedOperationException.class, () -> pushSdk.enviados().clear());
    }
}
