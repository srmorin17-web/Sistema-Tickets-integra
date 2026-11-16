-- ==========================================
-- ESQUEMA: permiso
-- ==========================================
CREATE TABLE permiso.rol (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) UNIQUE NOT NULL,
    descripcion VARCHAR(500)
);

CREATE TABLE permiso.permiso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) UNIQUE NOT NULL,
    descripcion VARCHAR(500)
);

CREATE TABLE permiso.rol_permiso (
    id_rol UUID REFERENCES permiso.rol(id) ON DELETE CASCADE,
    id_permiso UUID REFERENCES permiso.permiso(id) ON DELETE CASCADE,
    PRIMARY KEY (id_rol, id_permiso)
);

-- ==========================================
-- ESQUEMA: organigrama
-- ==========================================
CREATE TABLE organigrama.area (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500)
);

CREATE TABLE organigrama.departamento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_area UUID REFERENCES organigrama.area(id) ON DELETE RESTRICT,
    nombre VARCHAR(150) NOT NULL,
    descripcion VARCHAR(500)
);

-- ==========================================
-- ESQUEMA: empleado (Solo Lectura desde RRHH, pero con estructura local)
-- ==========================================
CREATE TABLE empleado.empleado (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_departamento UUID REFERENCES organigrama.departamento(id),
    lider_id UUID REFERENCES empleado.empleado(id), -- REFIRNAMIENTO: Para copia a líder jerárquico
    dni VARCHAR(20) UNIQUE NOT NULL,
    nombres VARCHAR(150) NOT NULL,
    apellidos VARCHAR(150) NOT NULL,
    cargo VARCHAR(150) NOT NULL,
    correo_personal VARCHAR(255),
    correo_corporativo VARCHAR(255) UNIQUE NOT NULL,
    fecha_ingreso DATE NOT NULL,
    estado_laboral VARCHAR(50) NOT NULL,
    fecha_modificacion TIMESTAMPTZ
);

-- ==========================================
-- ESQUEMA: usuario
-- ==========================================
CREATE TABLE usuario.usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_rol UUID REFERENCES permiso.rol(id) ON DELETE RESTRICT,
    id_empleado UUID UNIQUE REFERENCES empleado.empleado(id) ON DELETE CASCADE,
    contrasena_hash VARCHAR(255) NOT NULL,
    fecha_creacion TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    fecha_modificacion TIMESTAMPTZ
);

CREATE TABLE usuario.sesion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID REFERENCES usuario.usuario(id) ON DELETE CASCADE,
    refresh_token TEXT NOT NULL,
    fecha_creacion TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMPTZ NOT NULL
);

CREATE TABLE usuario.enlace_contrasena (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID REFERENCES usuario.usuario(id) ON DELETE CASCADE,
    fecha_envio TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    fecha_expiracion TIMESTAMPTZ NOT NULL,
    utilizado BOOLEAN DEFAULT FALSE
);

CREATE TABLE usuario.intento_acceso (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID REFERENCES usuario.usuario(id) ON DELETE CASCADE,
    ruta_intentada VARCHAR(500) NOT NULL,
    metodo_http VARCHAR(20) NOT NULL,
    fecha_intento TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ip_origen VARCHAR(50),
    resultado VARCHAR(50) NOT NULL
);

CREATE TABLE usuario.bloqueo_usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    id_usuario UUID REFERENCES usuario.usuario(id) ON DELETE CASCADE,
    fecha_inicio TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    fecha_fin TIMESTAMPTZ,
    motivo VARCHAR(500),
    activo BOOLEAN DEFAULT TRUE
);

-- ==========================================
-- ESQUEMA: ticket
-- ==========================================
CREATE TABLE ticket.estado (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(50) UNIQUE NOT NULL,
    descripcion VARCHAR(255),
    color_hex VARCHAR(7) NOT NULL,
    orden INT NOT NULL,
    es_estado_final BOOLEAN DEFAULT FALSE,
    activo BOOLEAN DEFAULT TRUE
);

CREATE TABLE ticket.categoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(100) UNIQUE NOT NULL,
    descripcion VARCHAR(500),
    activo BOOLEAN DEFAULT TRUE
);

CREATE TABLE ticket.prioridad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(50) UNIQUE NOT NULL,
    sla_horas INT NOT NULL
);

CREATE TABLE ticket.area_escalamiento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nombre VARCHAR(150) UNIQUE NOT NULL,
    contacto_responsable_id UUID REFERENCES empleado.empleado(id),
    descripcion VARCHAR(500),
    activo BOOLEAN DEFAULT TRUE
);

CREATE TABLE ticket.ticket (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(20) UNIQUE NOT NULL,
    asunto VARCHAR(200) NOT NULL,
    descripcion TEXT NOT NULL,
    solicitante_id UUID REFERENCES empleado.empleado(id) ON DELETE RESTRICT,
    tecnico_asignado_id UUID REFERENCES usuario.usuario(id),
    categoria_id UUID REFERENCES ticket.categoria(id),
    prioridad_id UUID REFERENCES ticket.prioridad(id),
    estado_id UUID REFERENCES ticket.estado(id),
    area_escalada_id UUID REFERENCES ticket.area_escalamiento(id),
    motivo_escalamiento TEXT,
    comentario_cierre TEXT,
    fecha_creacion TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMPTZ,
    fecha_cierre TIMESTAMPTZ
);

CREATE TABLE ticket.comentario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID REFERENCES ticket.ticket(id) ON DELETE CASCADE,
    usuario_id UUID REFERENCES usuario.usuario(id) ON DELETE RESTRICT,
    mensaje TEXT NOT NULL,
    es_interno BOOLEAN DEFAULT FALSE,
    fecha_creacion TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ticket.adjunto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID REFERENCES ticket.ticket(id) ON DELETE CASCADE,
    comentario_id UUID REFERENCES ticket.comentario(id) ON DELETE CASCADE,
    nombre_archivo VARCHAR(255) NOT NULL,
    ruta_cloudinary VARCHAR(500) NOT NULL,
    tipo_mime VARCHAR(50) NOT NULL,
    tamano_bytes INT NOT NULL,
    subido_por_id UUID REFERENCES usuario.usuario(id) ON DELETE RESTRICT,
    fecha_subida TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE ticket.historial_auditoria (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    ticket_id UUID REFERENCES ticket.ticket(id) ON DELETE CASCADE,
    usuario_id UUID REFERENCES usuario.usuario(id) ON DELETE RESTRICT,
    tipo_accion VARCHAR(100) NOT NULL,
    estado_anterior_id UUID REFERENCES ticket.estado(id),
    estado_nuevo_id UUID REFERENCES ticket.estado(id),
    datos_anteriores JSONB,
    datos_nuevos JSONB,
    timestamp TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    ip_origen VARCHAR(50)
);

CREATE TABLE ticket.notificacion (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id UUID REFERENCES usuario.usuario(id) ON DELETE CASCADE,
    ticket_id UUID REFERENCES ticket.ticket(id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL,
    asunto VARCHAR(200) NOT NULL,
    mensaje TEXT NOT NULL,
    estado_envio VARCHAR(50) NOT NULL,
    fecha_envio TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Índices para optimizar consultas frecuentes (RNF-01, RNF-02)
CREATE INDEX idx_ticket_estado ON ticket.ticket(estado_id);
CREATE INDEX idx_ticket_tecnico ON ticket.ticket(tecnico_asignado_id);
CREATE INDEX idx_ticket_solicitante ON ticket.ticket(solicitante_id);
CREATE INDEX idx_auditoria_ticket ON ticket.historial_auditoria(ticket_id);