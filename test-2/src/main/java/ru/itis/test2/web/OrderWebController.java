package ru.itis.test2.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.itis.test2.dto.CreateOrderRequest;
import ru.itis.test2.dto.OrderItemRequest;
import ru.itis.test2.dto.OrderResponse;
import ru.itis.test2.model.OrderStatus;
import ru.itis.test2.service.OrderService;
import ru.itis.test2.web.form.OrderForm;
import ru.itis.test2.web.form.OrderItemForm;

import java.util.UUID;

@Controller
@RequestMapping("/ui/orders")
@RequiredArgsConstructor
public class OrderWebController {

    private final OrderService orderService;

    @GetMapping
    public String list(@RequestParam(required = false) OrderStatus status,
                       @PageableDefault(size = 10, sort = "orderDate") Pageable pageable,
                       Model model) {
        Page<OrderResponse> orders = orderService.getOrders(status, pageable);
        model.addAttribute("orders", orders);
        model.addAttribute("status", status);
        model.addAttribute("statuses", OrderStatus.values());
        return "orders";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable UUID id, Model model) {
        model.addAttribute("order", orderService.getOrder(id));
        model.addAttribute("statuses", OrderStatus.values());
        return "order-details";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        OrderForm form = new OrderForm();
        form.getItems().add(new OrderItemForm());
        model.addAttribute("orderForm", form);
        return "order-new";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("orderForm") OrderForm form,
                         BindingResult bindingResult,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "order-new";
        }
        CreateOrderRequest request = new CreateOrderRequest(
                form.getCustomerName(),
                form.getItems().stream()
                        .map(i -> new OrderItemRequest(i.getProductName(), i.getQuantity(), i.getPrice()))
                        .toList());
        OrderResponse created = orderService.createOrder(request);
        redirectAttributes.addFlashAttribute("message", "Заказ создан");
        return "redirect:/ui/orders/" + created.id();
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable UUID id,
                               @RequestParam OrderStatus status,
                               RedirectAttributes redirectAttributes) {
        orderService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("message", "Статус обновлён: " + status);
        return "redirect:/ui/orders/" + id;
    }
}
