-- Habilitar extensión para UUIDs (nativa en Postgres 13+)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Creación de esquemas
CREATE SCHEMA IF NOT EXISTS permiso;
CREATE SCHEMA IF NOT EXISTS organigrama;
CREATE SCHEMA IF NOT EXISTS empleado;
CREATE SCHEMA IF NOT EXISTS usuario;
CREATE SCHEMA IF NOT EXISTS ticket;

-- Conceder permisos básicos al usuario de la aplicación
GRANT USAGE ON SCHEMA permiso, organigrama, empleado, usuario, ticket TO postgres;
GRANT ALL ON SCHEMA permiso, organigrama, empleado, usuario, ticket TO postgres;