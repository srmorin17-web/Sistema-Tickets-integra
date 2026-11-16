\set ON_ERROR_STOP off
\echo '--- auditoria_usuario: INSERT funciona; UPDATE y DELETE fallan'
INSERT INTO usuario.auditoria_usuario (id_actor, id_afectado, accion, detalle, ip_origen)
SELECT id, id, 'CAMBIO_CONTRASENA', NULL, '10.0.0.1' FROM usuario.usuario LIMIT 1;
UPDATE usuario.auditoria_usuario SET accion = 'X';
DELETE FROM usuario.auditoria_usuario;
SELECT count(*) AS filas_auditoria_usuario FROM usuario.auditoria_usuario;

\echo '--- bloqueo por intentos fallidos (ventana 15 min, máximo 5), con la misma lógica de AuthService'
DELETE FROM usuario.registro_acceso WHERE false;  -- (la tabla es inalterable: se usan correos distintos por escenario)
INSERT INTO usuario.registro_acceso (correo_intentado, ip_origen, resultado, fecha_hora) VALUES
 ('a@calma.org','1','FALLIDO', now()-interval '1 min'), ('a@calma.org','1','FALLIDO', now()-interval '2 min'), ('a@calma.org','1','FALLIDO', now()-interval '3 min'),
 ('a@calma.org','1','FALLIDO', now()-interval '4 min'), ('A@CALMA.ORG','1','FALLIDO', now()-interval '5 min'),                 -- escenario A: 5 fallidos recientes (una con otras mayúsculas)
 ('b@calma.org','1','FALLIDO', now()-interval '20 min'), ('b@calma.org','1','FALLIDO', now()-interval '21 min'), ('b@calma.org','1','FALLIDO', now()-interval '22 min'),
 ('b@calma.org','1','FALLIDO', now()-interval '23 min'), ('b@calma.org','1','FALLIDO', now()-interval '24 min'),                 -- escenario B: 5 fallidos pero fuera de la ventana
 ('c@calma.org','1','FALLIDO', now()-interval '8 min'), ('c@calma.org','1','FALLIDO', now()-interval '7 min'), ('c@calma.org','1','FALLIDO', now()-interval '6 min'),
 ('c@calma.org','1','FALLIDO', now()-interval '5 min'), ('c@calma.org','1','FALLIDO', now()-interval '4 min'),
 ('c@calma.org','1','EXITOSO', now()-interval '3 min'),                                                                         -- escenario C: 5 fallidos y luego entró bien
 ('d@calma.org','1','FALLIDO', now()-interval '3 min'), ('d@calma.org','1','FALLIDO', now()-interval '2 min'), ('d@calma.org','1','FALLIDO', now()-interval '1 min'),
 ('d@calma.org','1','BLOQUEADO', now()-interval '30 sec'), ('d@calma.org','1','BLOQUEADO', now()-interval '20 sec'), ('d@calma.org','1','BLOQUEADO', now()-interval '10 sec'); -- escenario D: 3 fallidos + 3 bloqueados (los bloqueados no cuentan)
WITH correos(c, esperado) AS (VALUES ('a@calma.org','BLOQUEADO'), ('b@calma.org','libre'), ('c@calma.org','libre'), ('d@calma.org','libre'), ('nadie@calma.org','libre')),
 ult AS (SELECT c, esperado, (SELECT max(fecha_hora) FROM usuario.registro_acceso r WHERE lower(r.correo_intentado)=lower(c) AND r.resultado='EXITOSO') AS ultimo_exito FROM correos),
 desde AS (SELECT c, esperado, CASE WHEN ultimo_exito IS NOT NULL AND ultimo_exito > now()-interval '15 min' THEN ultimo_exito ELSE now()-interval '15 min' END AS d FROM ult)
SELECT c AS correo, (SELECT count(*) FROM usuario.registro_acceso r WHERE lower(r.correo_intentado)=lower(desde.c) AND r.resultado='FALLIDO' AND r.fecha_hora > desde.d) AS fallidos_en_ventana,
       CASE WHEN (SELECT count(*) FROM usuario.registro_acceso r WHERE lower(r.correo_intentado)=lower(desde.c) AND r.resultado='FALLIDO' AND r.fecha_hora > desde.d) >= 5 THEN 'BLOQUEADO' ELSE 'libre' END AS resultado, esperado
  FROM desde ORDER BY 1;
\echo '--- último acceso exitoso por correo (consulta de la lista de usuarios)'
SELECT lower(correo_intentado) AS correo, max(fecha_hora) IS NOT NULL AS tiene_ultimo_acceso FROM usuario.registro_acceso WHERE resultado='EXITOSO' GROUP BY lower(correo_intentado);
