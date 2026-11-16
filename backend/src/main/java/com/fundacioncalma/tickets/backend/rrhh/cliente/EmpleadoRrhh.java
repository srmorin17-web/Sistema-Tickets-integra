package com.fundacioncalma.tickets.backend.rrhh.cliente;

import java.time.LocalDate;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Empleado tal como lo entrega la API de RR. HH. (contrato v1, ver docs/contrato-rrhh-v1.md).
 * Se ignoran los campos desconocidos para que RR. HH. pueda agregar datos sin romper la integración.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EmpleadoRrhh(
        UUID id,
        String nombres,
        String apellidos,
        @JsonAlias("correo_corporativo")
        String correoCorporativo,
        @JsonAlias("correo_personal")
        String correoPersonal,
        @JsonAlias("tipo_documento")
        String tipoDocumento,
        @JsonAlias("numero_documento")
        String numeroDocumento,
        String cargo,
        @JsonAlias("estado_laboral")
        String estadoLaboral,
        @JsonAlias("id_departamento")
        UUID idDepartamento,
        @JsonAlias("nombre_departamento")
        String nombreDepartamento,
        @JsonAlias("fecha_ingreso")
        LocalDate fechaIngreso,
        @JsonAlias("lider_id")
        UUID liderId,
        @JsonAlias("lider_correo")
        String liderCorreo) {}
