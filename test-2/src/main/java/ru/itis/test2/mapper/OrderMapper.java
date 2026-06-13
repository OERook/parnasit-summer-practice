package ru.itis.test2.mapper;

import org.springframework.stereotype.Component;
import ru.itis.test2.dto.OrderItemRequest;
import ru.itis.test2.dto.OrderItemResponse;
import ru.itis.test2.dto.OrderResponse;
import ru.itis.test2.model.Order;
import ru.itis.test2.model.OrderItem;

import java.util.List;

@Component
public class OrderMapper {

    public OrderItem toEntity(OrderItemRequest request) {
        return OrderItem.builder()
                .productName(request.productName())
                .quantity(request.quantity())
                .price(request.price())
                .build();
    }

    public OrderResponse toResponse(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getOrderDate(),
                order.getStatus(),
                toItemResponses(order.getItems())
        );
    }

    public OrderResponse toResponseWithoutItems(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerName(),
                order.getOrderDate(),
                order.getStatus(),
                null
        );
    }

    private List<OrderItemResponse> toItemResponses(List<OrderItem> items) {
        return items.stream()
                .map(i -> new OrderItemResponse(i.getId(), i.getProductName(), i.getQuantity(), i.getPrice()))
                .toList();
    }
}
