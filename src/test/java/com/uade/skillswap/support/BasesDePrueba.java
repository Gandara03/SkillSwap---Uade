package com.uade.skillswap.support;

import com.uade.skillswap.infrastructure.persistence.BaseDeDatos;
import com.uade.skillswap.infrastructure.persistence.JpaContexto;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Base nueva por test para los tests de persistencia. Por defecto H2 en memoria (lo que corre verify).
 * Con -Dskillswap.test.db=mysql (perfil Maven "mysql") crea una base MySQL skillswap_test_<uuid>, la migra
 * igual que la aplicacion y la borra al cerrar el contexto.
 * Variables: SKILLSWAP_TEST_DB_URL (servidor sin base, por defecto jdbc:mysql://localhost:3306/),
 * SKILLSWAP_TEST_DB_USER y SKILLSWAP_TEST_DB_PASSWORD (si faltan, se usan las SKILLSWAP_DB_*).
 */
public final class BasesDePrueba {

    private static final String SERVIDOR_POR_DEFECTO = "jdbc:mysql://localhost:3306/";

    private BasesDePrueba() {
    }

    public static JpaContexto nueva() {
        if ("mysql".equals(System.getProperty("skillswap.test.db", "h2"))) {
            return nuevaMySql();
        }
        return BaseDeDatos.iniciarH2();
    }

    private static JpaContexto nuevaMySql() {
        String servidor = variable("SKILLSWAP_TEST_DB_URL", SERVIDOR_POR_DEFECTO);
        if (!servidor.endsWith("/")) {
            servidor = servidor + "/";
        }
        String usuario = variable("SKILLSWAP_TEST_DB_USER", variable("SKILLSWAP_DB_USER", "root"));
        String clave = variable("SKILLSWAP_TEST_DB_PASSWORD", variable("SKILLSWAP_DB_PASSWORD", ""));
        String base = "skillswap_test_" + UUID.randomUUID().toString().replace("-", "");
        String url = servidor;
        ejecutar(url, usuario, clave, "CREATE DATABASE " + base);
        Runnable borrar = () -> ejecutar(url, usuario, clave, "DROP DATABASE IF EXISTS " + base);
        try {
            return BaseDeDatos.iniciarMySql(url + base + "?serverTimezone=UTC", usuario, clave, borrar);
        } catch (RuntimeException e) {
            borrar.run(); // si fallo la migracion o la validacion, no dejar la base huerfana
            throw e;
        }
    }

    private static void ejecutar(String url, String usuario, String clave, String sql) {
        try (Connection c = DriverManager.getConnection(url, usuario, clave);
             Statement s = c.createStatement()) {
            s.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("No se pudo ejecutar '" + sql + "': " + e.getMessage(), e);
        }
    }

    private static String variable(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return valor == null ? porDefecto : valor;
    }
}
