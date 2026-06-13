package ru.itis.test2.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itis.test2.model.Order;
import ru.itis.test2.model.OrderStatus;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    Page<Order> findAllByStatus(OrderStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(UUID id);

    @Query("""
            SELECT COALESCE(SUM(i.price * i.quantity), 0)
            FROM Order o
            JOIN o.items i
            WHERE o.customerName = :customerName
            """)
    BigDecimal totalAmountByCustomerName(@Param("customerName") String customerName);
}
