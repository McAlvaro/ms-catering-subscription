package com.mcalvaro.mscatering.infrastructure.integration.inbound.bc5.delivery;

import com.mcalvaro.mscatering.domain.core.DomainException;

/**
 * Exception representing delivery canonical contract and processing failures for BC5 Logistics integration.
 */
public class DeliveryContractException extends DomainException {

    public DeliveryContractException(String code, String message) {
        super(code, message);
    }

    public static DeliveryContractException invalid(String message) {
        return new DeliveryContractException("PROJ-001", message);
    }

    public static DeliveryContractException deliveryDayNotFound(String message) {
        return new DeliveryContractException("DEL-001", message);
    }

    public static DeliveryContractException conflict(String message) {
        return new DeliveryContractException("DEL-002", message);
    }
}
