package com.fundacioncalma.tickets.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching; // <-- AGREGAR ESTO

@SpringBootApplication
@EnableScheduling // Habilita las tareas programadas (@Scheduled) para el cierre automático de tickets
@EnableCaching // <-- AGREGAR ESTO
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
