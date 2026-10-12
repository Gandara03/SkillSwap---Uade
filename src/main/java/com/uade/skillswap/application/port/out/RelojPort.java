package com.uade.skillswap.application.port.out;

import java.time.LocalDateTime;

/** Fuente de tiempo inyectable (permite tests deterministas). */
public interface RelojPort {
    LocalDateTime ahora();
}
