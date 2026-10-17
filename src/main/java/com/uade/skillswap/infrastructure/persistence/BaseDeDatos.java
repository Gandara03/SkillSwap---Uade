package com.uade.skillswap.infrastructure.persistence;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Arranque de la base: primero Flyway crea/actualiza el esquema (db/migration) y despues se crea el
 * EntityManagerFactory con propiedades programaticas. Hibernate solo VALIDA el esquema
 * (hbm2ddl=validate): nunca lo crea ni lo modifica, el esquema es responsabilidad de las migraciones.
 * Las credenciales no estan en persistence.xml: llegan por variables de entorno o por parametro.
 */
public final class BaseDeDatos {

    private static final Logger LOG = LoggerFactory.getLogger(BaseDeDatos.class);

    private static final String UNIDAD_PERSISTENCIA = "skillswapPU";
    private static final String MIGRACIONES = "classpath:db/migration";

    static final String URL_MYSQL_POR_DEFECTO =
            "jdbc:mysql://localhost:3306/skillswap?createDatabaseIfNotExist=true&serverTimezone=UTC";

    private BaseDeDatos() {
    }

    /** MySQL real. Variables: SKILLSWAP_DB_URL, SKILLSWAP_DB_USER (root) y SKILLSWAP_DB_PASSWORD (vacia). */
    public static JpaContexto iniciarMySql() {
        String url = variable("SKILLSWAP_DB_URL", URL_MYSQL_POR_DEFECTO);
        String usuario = variable("SKILLSWAP_DB_USER", "root");
        String clave = variable("SKILLSWAP_DB_PASSWORD", "");
        return iniciarMySql(url, usuario, clave, () -> { });
    }

    /**
     * MySQL con url y credenciales explicitas. {@code alCerrar} libera lo que haya creado quien llama
     * (los tests con el perfil mysql borran ahi su base de prueba).
     */
    public static JpaContexto iniciarMySql(String url, String usuario, String clave, Runnable alCerrar) {
        return iniciar(url, usuario, clave, "com.mysql.cj.jdbc.Driver", alCerrar);
    }

    /**
     * H2 en memoria con modo MySQL. El nombre es unico por llamada, asi que cada contexto tiene su propia base
     * (ideal para tests y demo). DB_CLOSE_DELAY=-1 la mantiene viva entre conexiones hasta {@code cerrar()}.
     */
    public static JpaContexto iniciarH2() {
        String url = "jdbc:h2:mem:skillswap_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        return iniciar(url, "sa", "", "org.h2.Driver", () -> apagarH2(url));
    }

    private static JpaContexto iniciar(String url, String usuario, String clave, String driver, Runnable alCerrar) {
        migrar(url, usuario, clave);
        EntityManagerFactory emf = Persistence.createEntityManagerFactory(UNIDAD_PERSISTENCIA,
                propiedades(url, usuario, clave, driver));
        return new JpaContexto(emf, alCerrar);
    }

    private static void migrar(String url, String usuario, String clave) {
        Flyway.configure()
                .dataSource(url, usuario, clave)
                .locations(MIGRACIONES)
                .cleanDisabled(true)
                .load()
                .migrate();
    }

    private static Map<String, Object> propiedades(String url, String usuario, String clave, String driver) {
        Map<String, Object> p = new HashMap<>();
        p.put("jakarta.persistence.jdbc.url", url);
        p.put("jakarta.persistence.jdbc.user", usuario);
        p.put("jakarta.persistence.jdbc.password", clave);
        p.put("jakarta.persistence.jdbc.driver", driver);
        // Esquema: solo validar (NUNCA create/update). Si una entidad no coincide con V1__esquema.sql, falla al arrancar.
        p.put("hibernate.hbm2ddl.auto", "validate");
        // Pool simple de Hibernate: alcanza para la demo y los tests (varios threads en los tests de concurrencia).
        p.put("hibernate.connection.pool_size", "10");
        // Red de seguridad contra N+1: las colecciones lazy se cargan en lotes en vez de una consulta por fila.
        p.put("hibernate.default_batch_fetch_size", "16");
        // Falla en vez de paginar en memoria si alguien combina JOIN FETCH de colecciones con setMaxResults.
        p.put("hibernate.query.fail_on_pagination_over_collection_fetch", "true");
        if (Boolean.parseBoolean(System.getenv("SKILLSWAP_SHOW_SQL"))) {
            p.put("hibernate.show_sql", "true");
        }
        return p;
    }

    private static void apagarH2(String url) {
        try (Connection c = DriverManager.getConnection(url, "sa", "");
             Statement s = c.createStatement()) {
            s.execute("SHUTDOWN");
        } catch (SQLException e) {
            LOG.warn("No se pudo apagar la base H2 en memoria: {}", e.getMessage());
        }
    }

    private static String variable(String nombre, String porDefecto) {
        String valor = System.getenv(nombre);
        return valor == null ? porDefecto : valor;
    }
}
