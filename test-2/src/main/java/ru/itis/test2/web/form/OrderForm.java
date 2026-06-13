package ru.itis.test2.web.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable form-backing object for the Thymeleaf "new order" page.
 */
@Data
public class OrderForm {

    @NotBlank(message = "Укажите имя клиента")
    private String customerName;

    @NotEmpty(message = "Добавьте хотя бы одну позицию")
    private List<@Valid OrderItemForm> items = new ArrayList<>();
}
