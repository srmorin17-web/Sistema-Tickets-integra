package com.fundacioncalma.tickets.backend.rrhh.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class RrhhRestClienteJsonTest {

    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void leeSnapshotSnakeCaseEntregadoPorRrhh() throws Exception {
        UUID id = UUID.randomUUID();
        UUID departamentoId = UUID.randomUUID();
        UUID liderId = UUID.randomUUID();
        String json = """
                {
                  "empleados": [{
                    "id": "%s",
                    "nombres": "Nombre",
                    "apellidos": "Apellido",
                    "correo_corporativo": "empleado@example.org",
                    "correo_personal": "personal@example.org",
                    "tipo_documento": "DNI",
                    "numero_documento": "12345678",
                    "cargo": "Analista",
                    "estado_laboral": "ACTIVO",
                    "id_departamento": "%s",
                    "nombre_departamento": "Tecnología",
                    "fecha_ingreso": "2020-03-15",
                    "lider_id": "%s",
                    "lider_correo": "jefe@example.org"
                  }],
                  "siguiente_id": "%s",
                  "cursor_cambios": 12,
                  "hay_mas": false
                }
                """.formatted(id, departamentoId, liderId, id);

        RrhhSnapshotPage pagina = mapper.readValue(json, RrhhSnapshotPage.class);

        assertEquals(12, pagina.cursorCambios());
        assertFalse(pagina.hayMas());
        assertEquals(id, pagina.siguienteId());
        assertNotNull(pagina.empleados().getFirst().id());
        assertEquals("empleado@example.org", pagina.empleados().getFirst().correoCorporativo());
        assertEquals("personal@example.org", pagina.empleados().getFirst().correoPersonal());
        assertEquals("DNI", pagina.empleados().getFirst().tipoDocumento());
        assertEquals("12345678", pagina.empleados().getFirst().numeroDocumento());
        assertEquals("ACTIVO", pagina.empleados().getFirst().estadoLaboral());
        assertEquals("Analista", pagina.empleados().getFirst().cargo());
        assertEquals(departamentoId, pagina.empleados().getFirst().idDepartamento());
        assertEquals("Tecnología", pagina.empleados().getFirst().nombreDepartamento());
        assertEquals(LocalDate.of(2020, 3, 15), pagina.empleados().getFirst().fechaIngreso());
        assertEquals(liderId, pagina.empleados().getFirst().liderId());
        assertEquals("jefe@example.org", pagina.empleados().getFirst().liderCorreo());
    }

    @Test
    void leeCambioDeleteEnSnakeCase() throws Exception {
        UUID id = UUID.randomUUID();
        String json = """
                {
                  "cambios": [{
                    "version": 13,
                    "tipo": "DELETE",
                    "id_empleado": "%s",
                    "empleado": null
                  }],
                  "cursor_cambios": 13,
                  "hay_mas": false
                }
                """.formatted(id);

        RrhhCambiosPage pagina = mapper.readValue(json, RrhhCambiosPage.class);

        assertEquals(13, pagina.cursor());
        assertFalse(pagina.hayMas());
        assertEquals(id, pagina.cambios().getFirst().idEmpleado());
    }
}
