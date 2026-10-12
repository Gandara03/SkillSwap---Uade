package com.uade.skillswap.infrastructure.persistence.jpa;

import com.uade.skillswap.application.exception.ConflictoConcurrenciaException;
import com.uade.skillswap.application.exception.ValidacionException;
import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.domain.model.vo.SkillCoins;
import com.uade.skillswap.domain.model.wallet.Wallet;
import com.uade.skillswap.infrastructure.persistence.ErroresJpa;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JpaTransaccionAdapterTest extends JpaTestBase {

    private Perfil perfilNuevo(String userId) {
        return Perfil.crear(userId, "Nombre", TipoPerfil.PROFESIONAL, ZoneId.of("UTC"), AHORA);
    }

    @Test
    @DisplayName("confirma el trabajo y devuelve su resultado")
    void commit() {
        Perfil guardado = transaccion.ejecutar(() -> perfiles.guardar(perfilNuevo("u-ana")));

        assertNotNull(guardado.getId());
        assertTrue(perfiles.buscarPorUserId("u-ana").isPresent());
        assertFalse(ctx.hayUnidadActiva(), "la unidad de trabajo se cierra al terminar");
    }

    @Test
    @DisplayName("la variante Runnable tambien confirma")
    void commitRunnable() {
        transaccion.ejecutar(() -> {
            perfiles.guardar(perfilNuevo("u-ana"));
        });

        assertTrue(perfiles.buscarPorUserId("u-ana").isPresent());
    }

    @Test
    @DisplayName("rollback: si el trabajo lanza una excepcion no queda nada guardado y la excepcion se propaga igual")
    void rollbackPorExcepcion() {
        IllegalStateException error = assertThrows(IllegalStateException.class, () -> transaccion.ejecutar(() -> {
            perfiles.guardar(perfilNuevo("u-ana"));
            throw new IllegalStateException("fallo a mitad de camino");
        }));

        assertEquals("fallo a mitad de camino", error.getMessage());
        assertTrue(perfiles.buscarPorUserId("u-ana").isEmpty());
        assertFalse(ctx.hayUnidadActiva(), "tambien se cierra la unidad cuando hay error");
    }

    @Test
    @DisplayName("atomicidad entre repositorios: perfil y wallet se confirman o se revierten juntos")
    void atomicidadEntreRepositorios() {
        assertThrows(IllegalStateException.class, () -> transaccion.ejecutar(() -> {
            Perfil p = perfiles.guardar(perfilNuevo("u-ana"));
            wallets.guardar(Wallet.abrir(p.getId(), SkillCoins.de(100), AHORA));
            throw new IllegalStateException("revertir todo");
        }));
        assertTrue(perfiles.listarEntidades().isEmpty());
        assertTrue(wallets.listarEntidades().isEmpty());

        transaccion.ejecutar(() -> {
            Perfil p = perfiles.guardar(perfilNuevo("u-ana"));
            wallets.guardar(Wallet.abrir(p.getId(), SkillCoins.de(100), AHORA));
        });
        assertEquals(1, perfiles.listarEntidades().size());
        assertEquals(1, wallets.listarEntidades().size());
    }

    @Test
    @DisplayName("las excepciones de aplicacion atraviesan la transaccion sin traducirse y la revierten")
    void excepcionesDeAplicacionNoSeTraducen() {
        ValidacionException error = assertThrows(ValidacionException.class, () -> transaccion.ejecutar(() -> {
            perfiles.guardar(perfilNuevo("u-ana"));
            throw new ValidacionException("dato invalido");
        }));

        assertEquals("dato invalido", error.getMessage());
        assertTrue(perfiles.buscarPorUserId("u-ana").isEmpty());
    }

    @Test
    @DisplayName("reentrante: un ejecutar anidado participa de la transaccion externa (si la externa falla, se revierte todo)")
    void reentrante() {
        assertThrows(IllegalStateException.class, () -> transaccion.ejecutar(() -> {
            transaccion.ejecutar(() -> {
                perfiles.guardar(perfilNuevo("u-ana"));
            });
            assertTrue(ctx.hayUnidadActiva(), "el ejecutar interno no cierra la unidad externa");
            throw new IllegalStateException("falla el externo");
        }));
        assertTrue(perfiles.buscarPorUserId("u-ana").isEmpty());

        transaccion.ejecutar(() -> {
            transaccion.ejecutar(() -> {
                perfiles.guardar(perfilNuevo("u-bruno"));
            });
            perfiles.guardar(perfilNuevo("u-cecilia"));
        });
        assertEquals(2, perfiles.listarEntidades().size());
    }

    @Test
    @DisplayName("dentro de la unidad de trabajo las lecturas ven las escrituras todavia no confirmadas")
    void lecturasVenEscriturasPendientes() {
        transaccion.ejecutar(() -> {
            Perfil p = perfiles.guardar(perfilNuevo("u-ana"));
            assertEquals(p.getId(), perfiles.buscarPorUserId("u-ana").orElseThrow().getId());
            assertEquals(p.getId(), perfiles.buscarPorId(p.getId()).orElseThrow().getId());
        });
    }

    @Test
    @DisplayName("una instancia guardada y modificada dentro de la misma unidad puede guardarse de nuevo sin falso conflicto")
    void guardarVariasVecesDentroDeLaUnidad() {
        Perfil ana = transaccion.ejecutar(() -> {
            Perfil p = perfiles.guardar(perfilNuevo("u-ana"));
            p.darDeBaja();
            return perfiles.guardar(p);
        });

        Perfil leida = perfiles.buscarPorId(ana.getId()).orElseThrow();
        assertFalse(leida.estaActivo());
        assertEquals(Long.valueOf(1), leida.getVersion(), "INSERT al persistir (IDENTITY) y un UPDATE al volver a guardar");
    }

    @Test
    @DisplayName("sin unidad de trabajo, cada llamada de repositorio es atomica por si sola")
    void sinUnidadCadaLlamadaEsAtomica() {
        perfiles.guardar(perfilNuevo("u-ana"));

        assertFalse(ctx.hayUnidadActiva());
        assertTrue(perfiles.buscarPorUserId("u-ana").isPresent());
    }

    @Test
    @DisplayName("ErroresJpa traduce un conflicto de version (aunque venga envuelto en RollbackException) y deja pasar el resto")
    void traduccionDeErrores() {
        RollbackException envuelto = new RollbackException("commit fallido", new OptimisticLockException("version vieja"));
        RuntimeException traducida = ErroresJpa.traducir(envuelto);
        assertTrue(traducida instanceof ConflictoConcurrenciaException);
        assertSame(envuelto, traducida.getCause());

        assertTrue(ErroresJpa.traducir(new OptimisticLockException("x")) instanceof ConflictoConcurrenciaException);

        IllegalStateException otra = new IllegalStateException("otra cosa");
        assertSame(otra, ErroresJpa.traducir(otra));
        RollbackException sinLock = new RollbackException("por otra causa", new IllegalArgumentException("x"));
        assertSame(sinLock, ErroresJpa.traducir(sinLock));
        assertFalse(ErroresJpa.esConflicto(new RuntimeException()));
    }
}
