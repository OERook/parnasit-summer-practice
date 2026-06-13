package ru.itis.test2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import ru.itis.test2.dto.CreateOrderRequest;
import ru.itis.test2.dto.CustomerTotalResponse;
import ru.itis.test2.dto.OrderResponse;
import ru.itis.test2.dto.UpdateOrderStatusRequest;
import ru.itis.test2.model.OrderStatus;
import ru.itis.test2.service.OrderService;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management API")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(summary = "List orders with optional status filter, pagination and sorting")
    public Page<OrderResponse> getOrders(
            @RequestParam(required = false) OrderStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "orderDate") Pageable pageable) {
        return orderService.getOrders(status, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an order with all its items")
    public OrderResponse getOrder(@PathVariable UUID id) {
        return orderService.getOrder(id);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update order status")
    public OrderResponse updateStatus(@PathVariable UUID id,
                                      @Valid @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateStatus(id, request.status());
    }

    @GetMapping("/total")
    @Operation(summary = "Total amount (SUM(price * quantity)) of all orders for a customer")
    public CustomerTotalResponse getTotalByCustomer(@RequestParam String customerName) {
        return orderService.getTotalAmountByCustomer(customerName);
    }
}
