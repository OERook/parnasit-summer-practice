package ru.itis.test2.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.itis.test2.config.RabbitConfig;
import ru.itis.test2.dto.OrderCreatedEvent;
import ru.itis.test2.model.OrderStatus;
import ru.itis.test2.repository.OrderRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedListener {

    private final OrderRepository orderRepository;

    @RabbitListener(queues = RabbitConfig.ORDER_CREATED_QUEUE)
    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received order.created event: orderId={}, customerName={}, totalAmount={}",
                event.orderId(), event.customerName(), event.totalAmount());

        orderRepository.findById(event.orderId()).ifPresentOrElse(
                order -> {
                    order.setStatus(OrderStatus.PROCESSING);
                    orderRepository.save(order);
                    log.info("Order {} moved to status {}", order.getId(), OrderStatus.PROCESSING);
                },
                () -> log.warn("Order {} from event not found in database", event.orderId())
        );
    }
}
