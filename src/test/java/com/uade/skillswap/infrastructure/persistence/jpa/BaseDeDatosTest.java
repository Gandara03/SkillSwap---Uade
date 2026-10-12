package com.uade.skillswap.infrastructure.persistence.jpa;

import com.uade.skillswap.domain.model.perfil.Perfil;
import com.uade.skillswap.domain.model.perfil.TipoPerfil;
import com.uade.skillswap.infrastructure.persistence.BaseDeDatos;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;
import com.uade.skillswap.infrastructure.persistence.jpa.repository.JpaPerfilRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Arranque de la base. Que iniciarH2() termine sin error ya prueba lo mas importante: Flyway aplico
 * V1__esquema.sql y Hibernate (hbm2ddl=validate) verifico que las entidades coinciden con ese esquema.
 */
class BaseDeDatosTest {

    @Test
    @DisplayName("iniciarH2 aplica las migraciones y Hibernate valida que el esquema coincide con las entidades")
    void iniciarH2_migraYValidaElEsquema() {
        JpaContexto ctx = BaseDeDatos.iniciarH2();
        try {
            assertTrue(ctx.emf().isOpen());
            assertFalse(ctx.hayUnidadActiva());
        } finally {
            ctx.cerrar();
        }
    }

    @Test
    @DisplayName("cada llamada a iniciarH2 crea una base independiente")
    void iniciarH2_cadaContextoTieneSuPropiaBase() {
        JpaContexto a = BaseDeDatos.iniciarH2();
        JpaContexto b = BaseDeDatos.iniciarH2();
        try {
            new JpaPerfilRepository(a).guardar(Perfil.crear("u-ana", "Ana", TipoPerfil.PROFESIONAL,
                    ZoneId.of("UTC"), LocalDateTime.of(2030, 1, 1, 10, 0)));

            assertTrue(new JpaPerfilRepository(a).buscarPorUserId("u-ana").isPresent());
            assertFalse(new JpaPerfilRepository(b).buscarPorUserId("u-ana").isPresent());
            assertEquals(1, new JpaPerfilRepository(a).listarEntidades().size());
            assertEquals(0, new JpaPerfilRepository(b).listarEntidades().size());
        } finally {
            a.cerrar();
            b.cerrar();
        }
    }

    @Test
    @DisplayName("cerrar() cierra el EntityManagerFactory")
    void cerrar_cierraElFactory() {
        JpaContexto ctx = BaseDeDatos.iniciarH2();
        ctx.cerrar();
        assertFalse(ctx.emf().isOpen());
    }
}
