package com.mcalvaro.mscatering.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entrypoint del microservicio de Suscripción y Calendario de Catering (BC3).
 * <p>
 * Es la única clase que conoce Spring Boot. Los módulos {@code domain} y
 * {@code application} permanecen como Java puro, respetando la regla de
 * dependencia de Clean Architecture: Infrastructure → Application → Domain.
 */
@SpringBootApplication
public class MsCateringApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsCateringApplication.class, args);
    }
}
