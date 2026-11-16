\echo '--- insertar un adjunto con MIME de Word (71 caracteres)'
INSERT INTO ticket.adjunto (ticket_id, nombre_archivo, ruta_cloudinary, public_id, tipo_mime, tamano_bytes, subido_por_id)
VALUES ((SELECT id FROM ticket.ticket WHERE codigo='TKT-NEW'), 'acta.docx', 'https://res.cloudinary.com/demo/raw/authenticated/v1/calma-tickets/TKT-NEW/x_acta.docx', 'calma-tickets/TKT-NEW/x_acta.docx',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 123456, '00000000-0000-0000-0000-0000000000b3');
SELECT nombre_archivo, length(tipo_mime) AS largo_mime, purgado, fecha_subida IS NOT NULL AS con_fecha FROM ticket.adjunto;
\echo '--- public_id es obligatorio (debe fallar)'
\set ON_ERROR_STOP off
INSERT INTO ticket.adjunto (ticket_id, nombre_archivo, ruta_cloudinary, tipo_mime, tamano_bytes) VALUES ((SELECT id FROM ticket.ticket WHERE codigo='TKT-NEW'), 'x.pdf', 'u', 'application/pdf', 1);
\echo '--- purgar: se marca y la ruta queda vacía (permitido por NOT NULL)'
UPDATE ticket.adjunto SET purgado = true, fecha_purgado = now(), ruta_cloudinary = '' WHERE nombre_archivo = 'acta.docx';
SELECT purgado, ruta_cloudinary = '' AS ruta_vacia FROM ticket.adjunto;
\echo '--- índice por ticket'
SELECT indexname FROM pg_indexes WHERE tablename = 'adjunto' AND schemaname = 'ticket';
