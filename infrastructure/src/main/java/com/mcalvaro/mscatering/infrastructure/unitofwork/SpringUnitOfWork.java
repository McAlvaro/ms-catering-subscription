package com.mcalvaro.mscatering.infrastructure.unitofwork;

import com.mcalvaro.mscatering.application.abstractions.UnitOfWork;

import org.springframework.stereotype.Component;

/**
 * Implementación de {@link UnitOfWork} respaldada por Spring.
 * <p>
 * En esta primera versión, la transaccionalidad la maneja
 * {@code @Transactional}
 * a nivel del controlador REST. El método {@code commit()} queda como hook
 * explícito para:
 * <ul>
 * <li>Futura publicación de eventos de dominio acumulados en los
 * agregados.</li>
 * <li>Flush manual del EntityManager si fuera necesario.</li>
 * </ul>
 * <p>
 * Este diseño sigue el principio Open/Closed (OCP): cuando se necesite
 * despachar eventos, se extiende esta implementación sin modificar los
 * handlers.
 */
@Component
public class SpringUnitOfWork implements UnitOfWork {

    /**
     * Confirma los cambios pendientes.
     * <p>
     * Actualmente no-op porque {@code @Transactional} maneja el commit de JPA.
     * En futuras iteraciones, aquí se recolectarán y publicarán los
     * {@code DomainEvent} acumulados en los agregados modificados.
     */
    @Override
    public void commit() {
        // Spring @Transactional se encarga del commit de la transacción.
        // Futura extensión: despachar Domain Events aquí.
    }
}
