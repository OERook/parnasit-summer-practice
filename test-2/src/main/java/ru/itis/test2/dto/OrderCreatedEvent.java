package ru.itis.test2.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        String customerName,
        BigDecimal totalAmount
) {
}
