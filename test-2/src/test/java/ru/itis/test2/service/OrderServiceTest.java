package ru.itis.test2.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import ru.itis.test2.config.RabbitConfig;
import ru.itis.test2.dto.CreateOrderRequest;
import ru.itis.test2.dto.CustomerTotalResponse;
import ru.itis.test2.dto.OrderCreatedEvent;
import ru.itis.test2.dto.OrderItemRequest;
import ru.itis.test2.dto.OrderResponse;
import ru.itis.test2.exception.OrderNotFoundException;
import ru.itis.test2.mapper.OrderMapper;
import ru.itis.test2.model.Order;
import ru.itis.test2.model.OrderStatus;
import ru.itis.test2.repository.OrderRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Spy
    private OrderMapper orderMapper = new OrderMapper();

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_savesOrderAndPublishesEvent() {
        CreateOrderRequest request = new CreateOrderRequest("Alice", List.of(
                new OrderItemRequest("Laptop", 2, new BigDecimal("1000.00")),
                new OrderItemRequest("Mouse", 1, new BigDecimal("50.00"))
        ));
        UUID generatedId = UUID.randomUUID();
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(generatedId);
            return order;
        });

        OrderResponse response = orderService.createOrder(request);

        assertThat(response.id()).isEqualTo(generatedId);
        assertThat(response.customerName()).isEqualTo("Alice");
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.items()).hasSize(2);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitConfig.ORDER_EXCHANGE),
                eq(RabbitConfig.ORDER_CREATED_ROUTING_KEY),
                eventCaptor.capture());
        OrderCreatedEvent event = eventCaptor.getValue();
        assertThat(event.orderId()).isEqualTo(generatedId);
        assertThat(event.customerName()).isEqualTo("Alice");
        assertThat(event.totalAmount()).isEqualByComparingTo("2050.00");
    }

    @Test
    void getOrder_throwsNotFound_whenOrderDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(orderRepository.findWithItemsById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(id))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining(id.toString());
    }

    @Test
    void updateStatus_changesStatus() {
        UUID id = UUID.randomUUID();
        Order order = Order.builder()
                .id(id)
                .customerName("Bob")
                .status(OrderStatus.CREATED)
                .build();
        when(orderRepository.findWithItemsById(id)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.updateStatus(id, OrderStatus.COMPLETED);

        assertThat(response.status()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void getTotalAmountByCustomer_returnsAggregatedSum() {
        when(orderRepository.totalAmountByCustomerName("Alice")).thenReturn(new BigDecimal("2050.00"));

        CustomerTotalResponse response = orderService.getTotalAmountByCustomer("Alice");

        assertThat(response.customerName()).isEqualTo("Alice");
        assertThat(response.totalAmount()).isEqualByComparingTo("2050.00");
    }
}
