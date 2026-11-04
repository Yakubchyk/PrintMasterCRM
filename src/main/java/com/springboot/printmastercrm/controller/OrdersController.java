package com.springboot.printmastercrm.controller;

import com.springboot.printmastercrm.entity.*;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Раздел «Заказы»: клиенты с заказами → заказы клиента с подробностями; поиск по всем заказам;
 * оформление нового заказа (сначала выбор работ, затем расчёты и материалы).
 */
@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrdersController {

    private final OrderService orderService;
    private final CustomerService customerService;
    private final PostPressService postPressService;
    private final PrintingService printingService;
    private final ClicheService clicheService;
    private final SettingService settingService;
    private final MaterialService materialService;

    /** Без запроса — клиенты со сводкой по заказам; с запросом — найденные заказы целиком. */
    @GetMapping
    public String list(@RequestParam(value = "q", required = false) String q, Model model) {
        String query = q == null ? "" : q.trim();
        model.addAttribute("q", query);
        model.addAttribute("statuses", OrderStatus.values());
        if (query.isEmpty()) {
            model.addAttribute("summaries", orderService.summaries(customerService.search(null)));
        } else {
            List<Order> found = orderService.search(query);
            model.addAttribute("orders", found);
            model.addAttribute("consumption", orderService.consumptionByOrder(found));
        }
        return "orders";
    }

    @GetMapping("/customer/{id}")
    public String ofCustomer(@PathVariable Long id, Model model) {
        List<Order> orders = orderService.find(null, id);
        model.addAttribute("customer", customerService.get(id));
        model.addAttribute("orders", orders);
        model.addAttribute("consumption", orderService.consumptionByOrder(orders));
        model.addAttribute("statuses", OrderStatus.values());
        return "orders-customer";
    }

    // ---- оформление заказа ----

    @GetMapping("/new")
    public String newOrder(@RequestParam(value = "customerId", required = false) Long customerId, Model model) {
        if (customerId == null) {
            model.addAttribute("customers", customerService.search(null));
            return "order-new";
        }
        Setting settings = settingService.getSettings();
        model.addAttribute("customer", customerService.get(customerId));
        model.addAttribute("lu", settings.getLengthUnit());
        model.addAttribute("postPressList", postPressService.findByCustomerId(customerId));
        model.addAttribute("printingList", printingService.findByCustomerId(customerId));
        model.addAttribute("clicheList", clicheService.findByCustomerId(customerId));
        model.addAttribute("papers", PrintingService.PAPERS);
        model.addAttribute("colours", PrintingService.COLOURS);
        model.addAttribute("paperMaterials", materialService.findByCategory(MaterialCategory.PAPER));
        List<Material> clicheMaterials = new java.util.ArrayList<>(materialService.findByCategory(MaterialCategory.MAGNESIUM));
        clicheMaterials.addAll(materialService.findByCategory(MaterialCategory.BRASS));
        model.addAttribute("clicheMaterials", clicheMaterials);
        return "order-new";
    }

    /** Поля расхода материалов называются m_<id материала>; пустые и нулевые пропускаются. */
    @PostMapping
    public String create(@RequestParam Long customerId, @RequestParam String product,
                         @RequestParam(value = "postPressIds", required = false) List<Long> postPressIds,
                         @RequestParam(value = "printingIds", required = false) List<Long> printingIds,
                         @RequestParam(value = "clicheIds", required = false) List<Long> clicheIds,
                         @RequestParam Map<String, String> params) {
        orderService.createOrder(customerService.get(customerId), product, postPressIds, printingIds, clicheIds,
                parseConsumption(params));
        return "redirect:/orders/customer/" + customerId;
    }

    // ---- расчёты: размеры вводятся в выбранных единицах и хранятся в сантиметрах ----

    @PostMapping("/new/{id}/postpress")
    public String addPostPress(@PathVariable Long id, @RequestParam BigDecimal width,
                               @RequestParam BigDecimal length, @RequestParam int quantity) {
        LengthUnit unit = settingService.getSettings().getLengthUnit();
        postPressService.create(customerService.get(id), unit.toCm(width), unit.toCm(length), quantity);
        return backToNew(id);
    }

    @PostMapping("/new/{id}/postpress/{calcId}/delete")
    public String deletePostPress(@PathVariable Long id, @PathVariable Long calcId) {
        postPressService.delete(id, calcId);
        return backToNew(id);
    }

    @PostMapping("/new/{id}/printing")
    public String addPrinting(@PathVariable Long id, @RequestParam String papier,
                              @RequestParam String colour, @RequestParam int quantity) {
        printingService.create(customerService.get(id), papier, colour, quantity);
        return backToNew(id);
    }

    @PostMapping("/new/{id}/printing/{calcId}/delete")
    public String deletePrinting(@PathVariable Long id, @PathVariable Long calcId) {
        printingService.delete(id, calcId);
        return backToNew(id);
    }

    @PostMapping("/new/{id}/cliche")
    public String addCliche(@PathVariable Long id, @RequestParam BigDecimal width, @RequestParam BigDecimal length,
                            @RequestParam(value = "margin", required = false) BigDecimal margin,
                            @RequestParam int quantity,
                            @RequestParam(value = "materialId", required = false) Long materialId) {
        LengthUnit unit = settingService.getSettings().getLengthUnit();
        Material material = materialId == null ? null : materialService.get(materialId);
        clicheService.create(customerService.get(id), unit.toCm(width), unit.toCm(length),
                margin == null ? BigDecimal.ZERO : unit.toCm(margin), quantity, material);
        return backToNew(id);
    }

    @PostMapping("/new/{id}/cliche/{calcId}/delete")
    public String deleteCliche(@PathVariable Long id, @PathVariable Long calcId) {
        clicheService.delete(id, calcId);
        return backToNew(id);
    }

    // ---- действия над заказом ----

    @PostMapping("/{id}/status")
    public String setStatus(@PathVariable Long id, @RequestParam String status) {
        Order order = orderService.get(id);
        try {
            orderService.setStatus(id, OrderStatus.valueOf(status));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Неизвестный статус заказа");
        }
        return "redirect:/orders/customer/" + order.getCustomer().getId();
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        Long customerId = orderService.get(id).getCustomer().getId();
        orderService.delete(id);
        return "redirect:/orders/customer/" + customerId;
    }

    private static String backToNew(Long customerId) {
        return "redirect:/orders/new?customerId=" + customerId;
    }

    static Map<Long, BigDecimal> parseConsumption(Map<String, String> params) {
        Map<Long, BigDecimal> consumption = new LinkedHashMap<>();
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (!param.getKey().startsWith("m_")) {
                continue;
            }
            String raw = param.getValue() == null ? "" : param.getValue().trim().replace(',', '.');
            if (raw.isEmpty()) {
                continue;
            }
            try {
                Long materialId = Long.valueOf(param.getKey().substring(2));
                BigDecimal quantity = new BigDecimal(raw);
                if (quantity.signum() < 0) {
                    throw new BusinessRuleException("Расход материала не может быть отрицательным");
                }
                if (quantity.signum() > 0) {
                    consumption.put(materialId, quantity);
                }
            } catch (NumberFormatException e) {
                throw new BusinessRuleException("Расход материала указан неверно: «" + raw + "»");
            }
        }
        return consumption;
    }
}
