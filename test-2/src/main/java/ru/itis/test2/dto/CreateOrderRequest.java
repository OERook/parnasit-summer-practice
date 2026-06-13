package ru.itis.test2.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        @NotBlank(message = "customerName must not be blank")
        String customerName,

        @NotEmpty(message = "items must not be empty")
        List<@Valid OrderItemRequest> items
) {
}
