-- =====================================================================
-- Bloque 12: integración con RR. HH. por REST
-- La tabla empleado.empleado pasa a ser una réplica de solo lectura de RR. HH. (se actualiza al consultar a RR. HH.).
-- El sistema de tickets no necesita el DNI ni la fecha de ingreso, y el contrato de integración no los entrega
-- (minimización de datos personales). Se permiten nulos; la restricción UNIQUE del DNI sigue vigente para los que existan.
-- =====================================================================
ALTER TABLE empleado.empleado ALTER COLUMN dni DROP NOT NULL;
ALTER TABLE empleado.empleado ALTER COLUMN fecha_ingreso DROP NOT NULL;
