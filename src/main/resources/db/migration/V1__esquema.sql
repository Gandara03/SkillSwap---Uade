-- SkillSwap: esquema inicial.
-- Portable a MySQL 8 y a H2 en modo MySQL (sin sintaxis exclusiva de ninguno): tipos BIGINT AUTO_INCREMENT,
-- VARCHAR, INT, BOOLEAN, DOUBLE, DECIMAL(19,2), DATETIME(6) y TIME; claves, unicos e indices con nombre.
-- Los nombres de tablas y columnas deben coincidir EXACTAMENTE con las entidades JPA
-- (infrastructure/persistence/jpa/entity): Hibernate corre con hbm2ddl=validate y no modifica el esquema.
-- Convenciones: version = @Version (optimistic locking); estados, tipos y canales se guardan como VARCHAR
-- con el nombre del enum; los ids de otros agregados son columnas con FK (los agregados se referencian por id).

-- ======================================================================
-- Perfil
-- ======================================================================
CREATE TABLE perfil (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id VARCHAR(100) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    nombre_visible VARCHAR(150) NOT NULL,
    biografia VARCHAR(2000),
    zona_horaria VARCHAR(64) NOT NULL,
    canal_preferido VARCHAR(20) NOT NULL,
    reputacion DOUBLE NOT NULL,
    cantidad_calificaciones INT NOT NULL,
    activo BOOLEAN NOT NULL,
    creado_en DATETIME(6) NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_perfil_user_id UNIQUE (user_id)
);

CREATE INDEX idx_perfil_reputacion ON perfil (reputacion);

CREATE TABLE perfil_habilidad (
    perfil_id BIGINT NOT NULL,
    habilidad VARCHAR(100) NOT NULL,
    PRIMARY KEY (perfil_id, habilidad),
    CONSTRAINT fk_perfil_habilidad_perfil FOREIGN KEY (perfil_id) REFERENCES perfil (id)
);

CREATE TABLE perfil_interes (
    perfil_id BIGINT NOT NULL,
    interes VARCHAR(100) NOT NULL,
    PRIMARY KEY (perfil_id, interes),
    CONSTRAINT fk_perfil_interes_perfil FOREIGN KEY (perfil_id) REFERENCES perfil (id)
);

CREATE TABLE perfil_disponibilidad (
    perfil_id BIGINT NOT NULL,
    posicion INT NOT NULL,
    dia VARCHAR(10) NOT NULL,
    desde TIME NOT NULL,
    hasta TIME NOT NULL,
    PRIMARY KEY (perfil_id, posicion),
    CONSTRAINT fk_perfil_disponibilidad_perfil FOREIGN KEY (perfil_id) REFERENCES perfil (id)
);

-- ======================================================================
-- Publicacion (agregado: franjas, etiquetas, habilidades, historial)
-- ======================================================================
CREATE TABLE publicacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tipo VARCHAR(20) NOT NULL,
    autor_id BIGINT NOT NULL,
    titulo VARCHAR(200),
    descripcion VARCHAR(4000),
    categoria VARCHAR(100),
    modalidad VARCHAR(20),
    duracion_minutos INT NOT NULL,
    cupo INT NOT NULL,
    cupo_ocupado INT NOT NULL,
    precio_base DECIMAL(19,2),
    politica_cancelacion VARCHAR(20) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    creada_en DATETIME(6) NOT NULL,
    publicada_en DATETIME(6),
    revision INT NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_publicacion_autor FOREIGN KEY (autor_id) REFERENCES perfil (id)
);

-- Indice de la busqueda: solo PUBLICADAS, por categoria y rango/orden de precio.
CREATE INDEX idx_publicacion_estado_categoria_precio ON publicacion (estado, categoria, precio_base);
CREATE INDEX idx_publicacion_autor ON publicacion (autor_id, creada_en);

CREATE TABLE publicacion_etiqueta (
    publicacion_id BIGINT NOT NULL,
    etiqueta VARCHAR(100) NOT NULL,
    PRIMARY KEY (publicacion_id, etiqueta),
    CONSTRAINT fk_publicacion_etiqueta_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicacion (id)
);

CREATE TABLE publicacion_habilidad (
    publicacion_id BIGINT NOT NULL,
    habilidad VARCHAR(100) NOT NULL,
    PRIMARY KEY (publicacion_id, habilidad),
    CONSTRAINT fk_publicacion_habilidad_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicacion (id)
);

CREATE INDEX idx_publicacion_habilidad_habilidad ON publicacion_habilidad (habilidad);

CREATE TABLE franja (
    id BIGINT NOT NULL AUTO_INCREMENT,
    publicacion_id BIGINT NOT NULL,
    inicio DATETIME(6) NOT NULL,
    fin DATETIME(6) NOT NULL,
    ocupada BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_franja_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicacion (id)
);

-- Filtro de disponibilidad: franjas libres de una publicacion dentro de un rango.
CREATE INDEX idx_franja_publicacion_ocupada_inicio ON franja (publicacion_id, ocupada, inicio);

CREATE TABLE historial_publicacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    publicacion_id BIGINT NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo VARCHAR(20) NOT NULL,
    actor_id BIGINT NOT NULL,
    motivo VARCHAR(1000),
    fecha DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_historial_publicacion_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicacion (id),
    CONSTRAINT fk_historial_publicacion_actor FOREIGN KEY (actor_id) REFERENCES perfil (id)
);

CREATE INDEX idx_historial_publicacion_publicacion ON historial_publicacion (publicacion_id, id);

-- ======================================================================
-- Contratacion (agregado: historial; la propuesta de reprogramacion va en columnas propuesta_*)
-- ======================================================================
CREATE TABLE contratacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    publicacion_id BIGINT NOT NULL,
    cliente_id BIGINT NOT NULL,
    proveedor_id BIGINT NOT NULL,
    franja_id BIGINT NOT NULL,
    mensaje VARCHAR(2000) NOT NULL,
    objetivo VARCHAR(2000) NOT NULL,
    precio_final DECIMAL(19,2) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    propuesta_franja_id BIGINT,
    propuesta_por_id BIGINT,
    propuesta_fecha DATETIME(6),
    creada_en DATETIME(6) NOT NULL,
    pagada_en DATETIME(6),
    completada_en DATETIME(6),
    cancelada_por_id BIGINT,
    version BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_contratacion_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicacion (id),
    CONSTRAINT fk_contratacion_cliente FOREIGN KEY (cliente_id) REFERENCES perfil (id),
    CONSTRAINT fk_contratacion_proveedor FOREIGN KEY (proveedor_id) REFERENCES perfil (id),
    CONSTRAINT fk_contratacion_franja FOREIGN KEY (franja_id) REFERENCES franja (id),
    CONSTRAINT fk_contratacion_propuesta_franja FOREIGN KEY (propuesta_franja_id) REFERENCES franja (id),
    CONSTRAINT fk_contratacion_propuesta_por FOREIGN KEY (propuesta_por_id) REFERENCES perfil (id),
    CONSTRAINT fk_contratacion_cancelada_por FOREIGN KEY (cancelada_por_id) REFERENCES perfil (id)
);

CREATE INDEX idx_contratacion_cliente ON contratacion (cliente_id, creada_en);
CREATE INDEX idx_contratacion_proveedor ON contratacion (proveedor_id, creada_en);
CREATE INDEX idx_contratacion_publicacion ON contratacion (publicacion_id);

CREATE TABLE historial_contratacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contratacion_id BIGINT NOT NULL,
    estado_anterior VARCHAR(20),
    estado_nuevo VARCHAR(20) NOT NULL,
    actor_id BIGINT NOT NULL,
    motivo VARCHAR(1000),
    fecha DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_historial_contratacion_contratacion FOREIGN KEY (contratacion_id) REFERENCES contratacion (id),
    CONSTRAINT fk_historial_contratacion_actor FOREIGN KEY (actor_id) REFERENCES perfil (id)
);

CREATE INDEX idx_historial_contratacion_contratacion ON historial_contratacion (contratacion_id, id);

-- ======================================================================
-- Wallet y movimientos (un wallet por perfil; el saldo lleva version para el optimistic locking)
-- ======================================================================
CREATE TABLE wallet (
    id BIGINT NOT NULL AUTO_INCREMENT,
    perfil_id BIGINT NOT NULL,
    saldo DECIMAL(19,2) NOT NULL,
    version BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_wallet_perfil UNIQUE (perfil_id),
    CONSTRAINT fk_wallet_perfil FOREIGN KEY (perfil_id) REFERENCES perfil (id)
);

CREATE TABLE movimiento_credito (
    id BIGINT NOT NULL AUTO_INCREMENT,
    perfil_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    monto DECIMAL(19,2) NOT NULL,
    saldo_posterior DECIMAL(19,2) NOT NULL,
    contratacion_id BIGINT,
    fecha DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_movimiento_perfil FOREIGN KEY (perfil_id) REFERENCES perfil (id),
    CONSTRAINT fk_movimiento_contratacion FOREIGN KEY (contratacion_id) REFERENCES contratacion (id)
);

CREATE INDEX idx_movimiento_perfil ON movimiento_credito (perfil_id, id);

-- ======================================================================
-- Calificacion (una por contratacion y autor)
-- ======================================================================
CREATE TABLE calificacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    contratacion_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    destinatario_id BIGINT NOT NULL,
    puntaje INT NOT NULL,
    comentario VARCHAR(1000),
    fecha DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_calificacion_contratacion_autor UNIQUE (contratacion_id, autor_id),
    CONSTRAINT fk_calificacion_contratacion FOREIGN KEY (contratacion_id) REFERENCES contratacion (id),
    CONSTRAINT fk_calificacion_autor FOREIGN KEY (autor_id) REFERENCES perfil (id),
    CONSTRAINT fk_calificacion_destinatario FOREIGN KEY (destinatario_id) REFERENCES perfil (id)
);

CREATE INDEX idx_calificacion_destinatario ON calificacion (destinatario_id, fecha);

-- ======================================================================
-- Notificacion (bandeja por destinatario)
-- ======================================================================
CREATE TABLE notificacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    destinatario_id BIGINT NOT NULL,
    tipo_evento VARCHAR(40) NOT NULL,
    mensaje VARCHAR(1000) NOT NULL,
    canal VARCHAR(20) NOT NULL,
    leida BOOLEAN NOT NULL,
    fecha DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notificacion_destinatario FOREIGN KEY (destinatario_id) REFERENCES perfil (id)
);

CREATE INDEX idx_notificacion_destinatario_leida ON notificacion (destinatario_id, leida);

-- ======================================================================
-- Promocion
-- ======================================================================
CREATE TABLE promocion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    publicacion_id BIGINT NOT NULL,
    descripcion VARCHAR(500),
    tipo VARCHAR(20) NOT NULL,
    valor DECIMAL(19,4) NOT NULL,
    vigente_desde DATETIME(6),
    vigente_hasta DATETIME(6),
    activa BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_promocion_publicacion FOREIGN KEY (publicacion_id) REFERENCES publicacion (id)
);

CREATE INDEX idx_promocion_publicacion_activa ON promocion (publicacion_id, activa);
