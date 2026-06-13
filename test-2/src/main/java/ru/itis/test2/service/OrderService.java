package ru.itis.test2.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itis.test2.config.RabbitConfig;
import ru.itis.test2.dto.CreateOrderRequest;
import ru.itis.test2.dto.CustomerTotalResponse;
import ru.itis.test2.dto.OrderCreatedEvent;
import ru.itis.test2.dto.OrderResponse;
import ru.itis.test2.exception.OrderNotFoundException;
import ru.itis.test2.mapper.OrderMapper;
import ru.itis.test2.model.Order;
import ru.itis.test2.model.OrderItem;
import ru.itis.test2.model.OrderStatus;
import ru.itis.test2.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .customerName(request.customerName())
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.CREATED)
                .build();
        request.items().stream()
                .map(orderMapper::toEntity)
                .forEach(order::addItem);

        Order saved = orderRepository.save(order);

        BigDecimal totalAmount = saved.getItems().stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        OrderCreatedEvent event = new OrderCreatedEvent(saved.getId(), saved.getCustomerName(), totalAmount);
        rabbitTemplate.convertAndSend(RabbitConfig.ORDER_EXCHANGE, RabbitConfig.ORDER_CREATED_ROUTING_KEY, event);
        log.info("Order {} created, event published to '{}'", saved.getId(), RabbitConfig.ORDER_CREATED_QUEUE);

        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrders(OrderStatus status, Pageable pageable) {
        Page<Order> page = status == null
                ? orderRepository.findAll(pageable)
                : orderRepository.findAllByStatus(status, pageable);
        return page.map(orderMapper::toResponseWithoutItems);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID id) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return orderMapper.toResponse(order);
    }

    @Transactional
    public OrderResponse updateStatus(UUID id, OrderStatus status) {
        Order order = orderRepository.findWithItemsById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        order.setStatus(status);
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public CustomerTotalResponse getTotalAmountByCustomer(String customerName) {
        BigDecimal total = orderRepository.totalAmountByCustomerName(customerName);
        return new CustomerTotalResponse(customerName, total);
    }
}
