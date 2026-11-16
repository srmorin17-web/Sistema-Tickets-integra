# Pruebas de apoyo

## Con PostgreSQL (verificadas con PostgreSQL 16 real)
1. Cree una base vacía y aplique `backend/src/main/resources/db/migration/V1..V13` en orden.
2. `postgres_datos_prueba.sql`: empleados, un técnico y tres tickets (Resuelto hace 80 h, Resuelto hace 10 h y En revisión hace 200 h).
3. `postgres_cierre_automatico.sql`: el mismo SQL del `TicketAutoCloseScheduler`. Debe cerrar solo el ticket de 80 h
   y devolver los datos para el aviso por correo; una segunda ejecución devuelve 0 filas.
4. `postgres_auditoria_inalterable.sql`: los UPDATE, DELETE y TRUNCATE sobre el historial y los registros de acceso deben fallar;
   los INSERT deben funcionar; incluye las consultas nativas de `EmpleadoRepository`.
5. `postgres_adjuntos.sql` (después de la V11): un adjunto con el MIME de Word (71 caracteres) debe caber, `public_id` es obligatorio y la purga
   deja el registro con la ruta vacía.
6. `postgres_sincronizacion_rrhh.sql` (después de la V12): el mismo SQL de `RrhhSincronizador`. Alta sin DNI, actualización que conserva el
   DNI y el líder locales, conflicto de correo con otro id (debe fallar) y uso de los empleados sincronizados como representante y solicitante.
7. `postgres_gestion_usuarios.sql` (después de la V13): la auditoría de cuentas es inalterable y la lógica de bloqueo por intentos fallidos
   (5 en 15 minutos) da el resultado esperado en cinco escenarios: fallidos recientes, fuera de la ventana, seguidos de un acceso exitoso,
   intentos ya bloqueados que no cuentan, y correo inexistente. Antes de ejecutarla, la migración V13 debe haber elevado a Superadmin a la cuenta semilla.
8. `../docs/reconciliar_admin.sql`: reemplaza al administrador semilla por el empleado de RR. HH. conservando su cuenta de usuario.

## Solo con un JDK (sin base de datos ni Spring)
- `ejecutar_prueba_almacenamiento.sh`: 33 comprobaciones del módulo de adjuntos (lista blanca de tipos y contenido, archivos renombrados,
  límites, almacenamiento local y adaptador de Cloudinary contra un servidor SIMULADO que valida la firma de forma independiente).
- `ejecutar_prueba_rrhh.sh`: 19 comprobaciones del cliente REST de RR. HH. y de la caché contra un RR. HH. SIMULADO. Requiere las librerías
  Jackson 2 (`libjackson2-*-java` o `~/.m2`).
- `ejecutar_prueba_contrasenas.sh`: 19 comprobaciones de la política de contraseñas (incluida la contraseña de la cuenta semilla, prohibida)
  y del generador de contraseñas temporales (20 000 generadas: todas válidas, únicas y sin caracteres ambiguos).
- `PruebaSlaCalculator.java`: 13 casos del cálculo de SLA en horas laborales. Copie `SlaCalculatorService` (sin `@Service`) al paquete `p`.
