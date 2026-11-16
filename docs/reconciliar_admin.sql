-- Reconciliación del administrador semilla (V4) con RR. HH.
-- La migración V4 crea a admin@fundacioncalma.com con un id aleatorio. Si RR. HH. conoce a esa persona con OTRO id,
-- el id de RR. HH. es el canónico y hay que reemplazar el empleado semilla. Hágalo UNA vez, antes de usar el sistema
-- (con tickets ya creados por esa persona habría que repuntar también ticket.solicitante_id).
-- Uso:  psql -v correo="'admin@fundacioncalma.com'" -v rrhh_id="'<id de RR. HH.>'" -f reconciliar_admin.sql
BEGIN;
-- 1) El empleado semilla libera el correo y se anota su id
CREATE TEMP TABLE semilla AS SELECT id FROM empleado.empleado WHERE correo_corporativo = :correo;
UPDATE empleado.empleado SET correo_corporativo = 'semilla-' || id || '@local.invalid' WHERE id IN (SELECT id FROM semilla);
-- 2) Se crea el empleado con el id de RR. HH. (la aplicación lo actualizará con sus datos al primer acceso)
INSERT INTO empleado.empleado (id, nombres, apellidos, cargo, correo_corporativo, estado_laboral)
SELECT :rrhh_id::uuid, nombres, apellidos, cargo, :correo, estado_laboral FROM empleado.empleado WHERE id IN (SELECT id FROM semilla);
-- 3) La cuenta de usuario pasa a apuntar al empleado de RR. HH.
UPDATE usuario.usuario SET id_empleado = :rrhh_id::uuid WHERE id_empleado IN (SELECT id FROM semilla);
-- 4) Se elimina el empleado semilla
DELETE FROM empleado.empleado WHERE id IN (SELECT id FROM semilla);
COMMIT;
