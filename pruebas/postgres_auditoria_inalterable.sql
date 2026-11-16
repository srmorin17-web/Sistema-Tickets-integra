\set ON_ERROR_STOP off
\echo '--- historial_auditoria: UPDATE (debe fallar)'
UPDATE ticket.historial_auditoria SET tipo_accion = 'X';
\echo '--- historial_auditoria: DELETE (debe fallar)'
DELETE FROM ticket.historial_auditoria;
\echo '--- historial_auditoria: TRUNCATE (debe fallar)'
TRUNCATE ticket.historial_auditoria;
\echo '--- borrar un ticket con historial (cascada; debe fallar)'
DELETE FROM ticket.ticket WHERE codigo = 'TKT-OLD';
\echo '--- registro_acceso: INSERT (debe funcionar), UPDATE y DELETE (deben fallar)'
INSERT INTO usuario.registro_acceso (id_usuario, correo_intentado, ip_origen, resultado) VALUES (NULL, 'x@y.com', '10.0.0.1', 'FALLIDO');
UPDATE usuario.registro_acceso SET resultado = 'EXITOSO';
DELETE FROM usuario.registro_acceso;
\echo '--- intento_acceso: INSERT (debe funcionar), DELETE (debe fallar)'
INSERT INTO usuario.intento_acceso (id_usuario, ruta_intentada, metodo_http, ip_origen, resultado) VALUES (NULL, '/api/v1/admin/catalogos', 'GET', '10.0.0.2', 'DENEGADO');
DELETE FROM usuario.intento_acceso;
\echo '--- filas que siguen existiendo'
SELECT (SELECT count(*) FROM ticket.historial_auditoria) AS historial, (SELECT count(*) FROM usuario.registro_acceso) AS registro_acceso, (SELECT count(*) FROM usuario.intento_acceso) AS intento_acceso, (SELECT count(*) FROM ticket.ticket WHERE codigo='TKT-OLD') AS ticket_old_sigue;
\echo '--- consultas nativas del EmpleadoRepository (con los parámetros reemplazados)'
SELECT e.id, e.nombres, e.apellidos, e.correo_corporativo AS "correoCorporativo", e.cargo FROM empleado.empleado e WHERE LOWER(e.nombres || ' ' || e.apellidos) LIKE LOWER(CONCAT('%', 'ANA', '%')) OR LOWER(e.correo_corporativo) LIKE LOWER(CONCAT('%', 'ANA', '%')) ORDER BY e.apellidos, e.nombres LIMIT 20;
SELECT l.correo_corporativo FROM empleado.empleado e JOIN empleado.empleado l ON l.id = e.lider_id WHERE e.id = '00000000-0000-0000-0000-0000000000a2';
SELECT e.id AS id, e.nombres AS nombres, e.apellidos AS apellidos, e.correo_corporativo AS "correoCorporativo", e.cargo AS cargo FROM empleado.empleado e WHERE e.id = '00000000-0000-0000-0000-0000000000a3';
