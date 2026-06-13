package ru.itis.test2.dto;

import java.math.BigDecimal;

public record CustomerTotalResponse(
        String customerName,
        BigDecimal totalAmount
) {
}
