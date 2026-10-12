package com.uade.skillswap.domain.query;

/** Pedido de pagina (base 0). El tamano maximo evita consultas sin limite. */
public record PaginaRequest(int pagina, int tamano) {

    public static final int TAMANO_MAXIMO = 100;

    public PaginaRequest {
        if (pagina < 0) {
            throw new IllegalArgumentException("La pagina no puede ser negativa");
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw new IllegalArgumentException("El tamano de pagina debe estar entre 1 y " + TAMANO_MAXIMO);
        }
    }

    public static PaginaRequest de(int pagina, int tamano) {
        return new PaginaRequest(pagina, tamano);
    }

    public int offset() {
        return pagina * tamano;
    }
}
