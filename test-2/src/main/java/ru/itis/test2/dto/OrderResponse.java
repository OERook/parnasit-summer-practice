package ru.itis.test2.dto;

import ru.itis.test2.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String customerName,
        LocalDateTime orderDate,
        OrderStatus status,
        List<OrderItemResponse> items
) {
}
