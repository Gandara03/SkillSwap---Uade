package com.uade.skillswap.acceptance.jpa;

import com.uade.skillswap.application.dto.ContratacionDto;
import com.uade.skillswap.application.dto.PublicacionDto;
import com.uade.skillswap.application.exception.ErrorCode;
import com.uade.skillswap.presentation.Resultado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Caso de aceptacion 4 (JPA/H2): evitar que dos aceptaciones consuman el ultimo cupo.
 *
 * <p>Dos pruebas:
 * <ul>
 *   <li>Secuencial: la segunda aceptacion falla con CUPO_AGOTADO y, como la transaccion se revierte de verdad,
 *       la solicitud rechazada sigue PENDIENTE y nadie perdio saldo.</li>
 *   <li>Concurrente: dos hilos (dos unidades de trabajo, cada una con su EntityManager) aceptan a la vez
 *       solicitudes distintas sobre la MISMA publicacion de cupo 1. Esperado: exactamente una se confirma y la otra
 *       falla, ya sea con CONFLICTO_CONCURRENCIA (ambas leyeron la misma version de la publicacion y la segunda
 *       en confirmar pierde por optimistic locking con @Version) o con CUPO_AGOTADO (una termino antes de que la
 *       otra leyera). Cualquiera de los dos finales es correcto; lo inadmisible es que las dos tengan exito.</li>
 * </ul>
 */
@DisplayName("Caso 4 (JPA/H2): evitar que dos aceptaciones consuman el ultimo cupo")
class Caso04UltimoCupoJpaTest extends AceptacionJpaBase {

    @Test
    @DisplayName("Caso 4: con cupo 1 y dos solicitudes, la segunda aceptacion falla y la transaccion se revierte")
    void caso04_dosAceptacionesNoConsumenElUltimoCupo() {
        profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        profesional("u-cecilia", "Cecilia");
        PublicacionDto mentoria = publicarMentoria("u-ana", 40, 1, 2);
        ContratacionDto deBruno = solicitar("u-bruno", mentoria, 0);
        ContratacionDto deCecilia = solicitar("u-cecilia", mentoria, 1);

        assertEquals("ACEPTADA", aceptar("u-ana", deBruno.id()).estado());
        como("u-ana");
        Resultado<ContratacionDto> segunda = app.getContratacionController().aceptar(deCecilia.id());

        assertTrue(segunda.esError());
        assertEquals(ErrorCode.CUPO_AGOTADO, segunda.codigo());
        // rollback real: la solicitud rechazada sigue PENDIENTE y la franja de Cecilia libre
        assertEquals("PENDIENTE", contratacionVista("u-cecilia", deCecilia.id()).estado());
        PublicacionDto tras = publicacionVista("u-ana", mentoria.id());
        assertEquals(0, tras.cupoDisponible());
        assertTrue(tras.franjas().get(0).ocupada());
        assertFalse(tras.franjas().get(1).ocupada());
        // saldos intactos
        assertEquals(0, sc(100).compareTo(saldo("u-bruno")));
        assertEquals(0, sc(100).compareTo(saldo("u-cecilia")));
        assertEquals(0, sc(100).compareTo(saldo("u-ana")));
    }

    @Test
    @DisplayName("Caso 4: dos aceptaciones simultaneas sobre la misma publicacion de cupo 1: solo una se confirma")
    void caso04_dosAceptacionesConcurrentesSoloUnaConsumeElCupo() throws Exception {
        profesional("u-ana", "Ana");
        profesional("u-bruno", "Bruno");
        profesional("u-cecilia", "Cecilia");
        PublicacionDto mentoria = publicarMentoria("u-ana", 40, 1, 2);
        ContratacionDto deBruno = solicitar("u-bruno", mentoria, 0);
        ContratacionDto deCecilia = solicitar("u-cecilia", mentoria, 1);
        como("u-ana"); // el proveedor es el mismo en los dos hilos: la sesion de identidad se comparte sin problema

        List<Resultado<ContratacionDto>> resultados = aceptarEnParalelo(deBruno.id(), deCecilia.id());

        // exactamente una acepto; la otra perdio por conflicto de version o por cupo agotado
        List<Resultado<ContratacionDto>> exitos = resultados.stream().filter(Resultado::esOk).toList();
        List<Resultado<ContratacionDto>> fallos = resultados.stream().filter(Resultado::esError).toList();
        assertEquals(1, exitos.size(), "una sola aceptacion debe confirmarse: " + resultados);
        assertEquals(1, fallos.size(), "la otra debe fallar: " + resultados);
        ErrorCode codigo = fallos.get(0).codigo();
        assertTrue(codigo == ErrorCode.CONFLICTO_CONCURRENCIA || codigo == ErrorCode.CUPO_AGOTADO,
                "se esperaba CONFLICTO_CONCURRENCIA o CUPO_AGOTADO y fue " + codigo);

        // el estado final es consistente: un cupo consumido, una franja ocupada, un ganador ACEPTADA y un perdedor PENDIENTE
        PublicacionDto tras = publicacionVista("u-ana", mentoria.id());
        assertEquals(0, tras.cupoDisponible());
        assertEquals(1L, tras.franjas().stream().filter(f -> f.ocupada()).count());
        List<String> estados = new ArrayList<>();
        estados.add(contratacionVista("u-bruno", deBruno.id()).estado());
        estados.add(contratacionVista("u-cecilia", deCecilia.id()).estado());
        assertTrue(estados.contains("ACEPTADA") && estados.contains("PENDIENTE"), "estados finales: " + estados);
        // nadie perdio saldo
        assertEquals(0, sc(100).compareTo(saldo("u-bruno")));
        assertEquals(0, sc(100).compareTo(saldo("u-cecilia")));
    }

    /** Lanza las dos aceptaciones desde hilos distintos y las libera a la vez para maximizar la superposicion. */
    private List<Resultado<ContratacionDto>> aceptarEnParalelo(Long contratacionA, Long contratacionB) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch listos = new CountDownLatch(2);
            CountDownLatch largada = new CountDownLatch(1);
            Callable<Resultado<ContratacionDto>> aceptarA = () -> {
                listos.countDown();
                largada.await();
                return app.getContratacionController().aceptar(contratacionA);
            };
            Callable<Resultado<ContratacionDto>> aceptarB = () -> {
                listos.countDown();
                largada.await();
                return app.getContratacionController().aceptar(contratacionB);
            };
            Future<Resultado<ContratacionDto>> a = pool.submit(aceptarA);
            Future<Resultado<ContratacionDto>> b = pool.submit(aceptarB);
            assertTrue(listos.await(30, TimeUnit.SECONDS), "los hilos no arrancaron");
            largada.countDown();
            return List.of(a.get(60, TimeUnit.SECONDS), b.get(60, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
    }
}
