package ru.itis.test2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.itis.test2.model.Order;
import ru.itis.test2.model.OrderStatus;
import ru.itis.test2.repository.OrderRepository;

import java.time.Duration;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void createOrder_persistsToDatabase_andListenerMovesStatusToProcessing() throws Exception {
        String body = """
                {
                  "customerName": "Integration Customer",
                  "items": [
                    {"productName": "Keyboard", "quantity": 2, "price": 49.90},
                    {"productName": "Monitor", "quantity": 1, "price": 300.00}
                  ]
                }
                """;

        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.customerName").value("Integration Customer"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andReturn();

        UUID orderId = extractId(result.getResponse().getContentAsString());

        // Order is persisted
        Order saved = orderRepository.findWithItemsById(orderId).orElseThrow();
        assertThat(saved.getCustomerName()).isEqualTo("Integration Customer");
        assertThat(saved.getItems()).hasSize(2);

        // The RabbitMQ listener consumes the order.created event and moves the order to PROCESSING
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Order updated = orderRepository.findById(orderId).orElseThrow();
            assertThat(updated.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        });

        // Custom aggregation query works end-to-end
        mockMvc.perform(get("/api/orders/total").param("customerName", "Integration Customer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(399.80));
    }

    @Test
    void getOrder_returns404_whenOrderDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void createOrder_returns400_whenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerName\": \"\", \"items\": []}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateStatus_changesOrderStatus() throws Exception {
        String body = """
                {
                  "customerName": "Status Customer",
                  "items": [{"productName": "Cable", "quantity": 1, "price": 5.00}]
                }
                """;
        MvcResult result = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        UUID orderId = extractId(result.getResponse().getContentAsString());

        mockMvc.perform(put("/api/orders/{id}/status", orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"COMPLETED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    private UUID extractId(String json) {
        Matcher matcher = Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-f-]{36})\"").matcher(json);
        assertThat(matcher.find()).isTrue();
        return UUID.fromString(matcher.group(1));
    }
}
