package ru.itis.test2.web.form;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemForm {

    @NotBlank(message = "Укажите название товара")
    private String productName;

    @NotNull(message = "Укажите количество")
    @Positive(message = "Количество должно быть положительным")
    private Integer quantity;

    @NotNull(message = "Укажите цену")
    @DecimalMin(value = "0.00", message = "Цена не может быть отрицательной")
    private BigDecimal price;
}
