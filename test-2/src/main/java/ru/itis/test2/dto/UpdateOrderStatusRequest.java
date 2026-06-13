package ru.itis.test2.dto;

import jakarta.validation.constraints.NotNull;
import ru.itis.test2.model.OrderStatus;

public record UpdateOrderStatusRequest(
        @NotNull(message = "status is required")
        OrderStatus status
) {
}
