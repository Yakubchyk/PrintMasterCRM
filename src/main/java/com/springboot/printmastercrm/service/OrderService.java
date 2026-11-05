package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.*;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.ClicheRepository;
import com.springboot.printmastercrm.repository.OrderMaterialRepository;
import com.springboot.printmastercrm.repository.OrderRepository;
import com.springboot.printmastercrm.repository.PostPressRepository;
import com.springboot.printmastercrm.repository.PrintingRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PostPressRepository postPressRepository;
    private final PrintingRepository printingRepository;
    private final ClicheRepository clicheRepository;
    private final OrderMaterialRepository orderMaterialRepository;
    private final MaterialService materialService;
    private final SettingService settingService;

    /** Сводка по клиенту для раздела «Заказы». */
    @Getter
    @AllArgsConstructor
    public static class CustomerSummary {
        private final Customer customer;
        private final int orderCount;
        /** Заказы, которые ещё не выполнены. */
        private final int activeCount;
        private final BigDecimal total;
        private final LocalDateTime lastOrderAt;
    }

    /**
     * Оформляет заказ из выбранных расчётов клиента. Суммы берутся из сохранённых расчётов,
     * из запроса принимаются только их id и описание. Расчёт другого клиента даёт NotFoundException.
     * Материалы резервируются на складе: вручную указанные в consumption (id материала -> количество)
     * и материал клише (площадь с полями × количество). Если свободного материала не хватает,
     * заказ не создаётся. Со склада материал списывается при отгрузке заказа.
     */
    @Transactional
    public Order createOrder(Customer customer, String product,
                             List<Long> postPressIds, List<Long> printingIds, List<Long> clicheIds,
                             Map<Long, BigDecimal> consumption) {
        if (product == null || product.isBlank()) {
            throw new BusinessRuleException("Укажите детали заказа");
        }
        LinkedHashSet<Long> ppIds = unique(postPressIds);
        LinkedHashSet<Long> prIds = unique(printingIds);
        LinkedHashSet<Long> clIds = unique(clicheIds);
        if (ppIds.isEmpty() && prIds.isEmpty() && clIds.isEmpty()) {
            throw new BusinessRuleException("Выберите хотя бы один расчёт для заказа");
        }
        LengthUnit lu = settingService.getSettings().getLengthUnit();
        List<String> lines = new ArrayList<>();
        Map<Long, BigDecimal> toReserve = new LinkedHashMap<>();
        if (consumption != null) {
            consumption.forEach((id, qty) -> {
                if (qty != null && qty.signum() != 0) {
                    toReserve.merge(id, qty, BigDecimal::add);
                }
            });
        }

        BigDecimal cliche = BigDecimal.ZERO;
        for (Long id : clIds) {
            Cliche cl = clicheRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Расчёт клише не найден"));
            requireSameCustomer(cl.getCustomer(), customer);
            cliche = cliche.add(cl.getTotalPrice());
            lines.add(describe(cl, lu));
            if (cl.getMaterial() != null) {
                toReserve.merge(cl.getMaterial().getId(),
                        cl.getTotalAreaCm2().setScale(3, RoundingMode.HALF_UP), BigDecimal::add);
            }
        }
        BigDecimal postpress = BigDecimal.ZERO;
        for (Long id : ppIds) {
            PostPress pp = postPressRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Расчёт тиснения не найден"));
            requireSameCustomer(pp.getCustomer(), customer);
            postpress = postpress.add(pp.getTotalPrice());
            lines.add("Тиснение: оттиск " + lu.format(pp.getWidthCm()) + " × " + lu.format(pp.getLengthCm()) + " "
                    + lu.getLabel() + ", тираж " + pp.getQuantity()
                    + (pp.getMaterial() != null ? ", " + pp.getMaterial().getName() : "")
                    + " — " + money(pp.getTotalPrice()));
            if (pp.getMaterial() != null) {
                toReserve.merge(pp.getMaterial().getId(),
                        pp.getFoilAreaCm2().setScale(3, RoundingMode.HALF_UP), BigDecimal::add);
            }
        }
        BigDecimal printing = BigDecimal.ZERO;
        for (Long id : prIds) {
            Printing pr = printingRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Расчёт печати не найден"));
            requireSameCustomer(pr.getCustomer(), customer);
            printing = printing.add(pr.getTotalCost());
            lines.add("Печать: бумага " + pr.getPapier() + ", " + pr.getColour() + ", тираж " + pr.getQuantity()
                    + " — " + money(pr.getTotalCost()));
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setProduct(product.trim());
        order.setDetails(String.join("\n", lines));
        order.setPostpressPrice(postpress.setScale(2, RoundingMode.HALF_UP));
        order.setPrintingPrice(printing.setScale(2, RoundingMode.HALF_UP));
        order.setClichePrice(cliche.setScale(2, RoundingMode.HALF_UP));
        order.setTotalCost(postpress.add(printing).add(cliche).setScale(2, RoundingMode.HALF_UP));
        order.setStatus(OrderStatus.NEW);
        order = orderRepository.save(order);

        for (Map.Entry<Long, BigDecimal> item : toReserve.entrySet()) {
            materialService.reserve(item.getKey(), item.getValue());
            OrderMaterial used = new OrderMaterial();
            used.setOrder(order);
            used.setMaterial(materialService.get(item.getKey()));
            used.setQuantity(item.getValue());
            used.setWrittenOff(false);
            orderMaterialRepository.save(used);
        }
        return order;
    }

    /** Статус «Выполнен» списывает зарезервированные материалы со склада; возврат из него снова ставит их в резерв. */
    @Transactional
    public void setStatus(Long orderId, OrderStatus status) {
        Order order = get(orderId);
        OrderStatus old = order.getStatus();
        if (old != OrderStatus.DONE && status == OrderStatus.DONE) {
            for (OrderMaterial used : orderMaterialRepository.findByOrderId(orderId)) {
                if (!used.isWrittenOff()) {
                    materialService.ship(used.getMaterial().getId(), used.getQuantity(), "Заказ №" + orderId + " выполнен");
                    used.setWrittenOff(true);
                    orderMaterialRepository.save(used);
                }
            }
        } else if (old == OrderStatus.DONE && status != OrderStatus.DONE) {
            for (OrderMaterial used : orderMaterialRepository.findByOrderId(orderId)) {
                if (used.isWrittenOff()) {
                    materialService.unship(used.getMaterial().getId(), used.getQuantity(),
                            "Отмена выполнения: заказ №" + orderId);
                    used.setWrittenOff(false);
                    orderMaterialRepository.save(used);
                }
            }
        }
        if (status == OrderStatus.DONE && old != OrderStatus.DONE) {
            order.setCompletedAt(LocalDateTime.now());
        } else if (status != OrderStatus.DONE) {
            order.setCompletedAt(null);
        }
        order.setStatus(status);
        orderRepository.save(order);
    }

    public Order get(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Заказ не найден"));
    }

    /** Удаляет заказ; резерв снимается, а уже списанные при отгрузке материалы возвращаются на склад. */
    @Transactional
    public void delete(Long orderId) {
        removeWithStockReturn(get(orderId));
    }

    /** Удаляет все заказы клиента с возвратом материалов на склад. */
    @Transactional
    public void deleteByCustomer(Long customerId) {
        for (Order order : orderRepository.findByCustomerIdOrderByIdDesc(customerId)) {
            removeWithStockReturn(order);
        }
    }

    private void removeWithStockReturn(Order order) {
        for (OrderMaterial used : orderMaterialRepository.findByOrderId(order.getId())) {
            Long materialId = used.getMaterial().getId();
            if (used.isWrittenOff()) {
                materialService.receive(materialId, used.getQuantity(), "Возврат: заказ №" + order.getId() + " удалён");
            } else {
                materialService.release(materialId, used.getQuantity());
            }
            orderMaterialRepository.delete(used);
        }
        orderRepository.delete(order);
    }

    /** Материалы по заказам: id заказа -> список. */
    public Map<Long, List<OrderMaterial>> consumptionByOrder(List<Order> orders) {
        Map<Long, List<OrderMaterial>> result = new HashMap<>();
        if (orders.isEmpty()) {
            return result;
        }
        List<Long> ids = orders.stream().map(Order::getId).toList();
        for (OrderMaterial used : orderMaterialRepository.findByOrderIdIn(ids)) {
            result.computeIfAbsent(used.getOrder().getId(), k -> new ArrayList<>()).add(used);
        }
        return result;
    }

    /** status == null означает «любой статус», customerId == null — «любой клиент». */
    public List<Order> find(OrderStatus status, Long customerId) {
        if (customerId == null) {
            return status == null ? orderRepository.findAllByOrderByIdDesc()
                    : orderRepository.findByStatusOrderByIdDesc(status);
        }
        return status == null ? orderRepository.findByCustomerIdOrderByIdDesc(customerId)
                : orderRepository.findByCustomerIdAndStatusOrderByIdDesc(customerId, status);
    }

    /**
     * Поиск по заказам: номер, клиент (название, телефон, email, город), описание, состав работ
     * и названия материалов. Пустой запрос возвращает все заказы.
     */
    public List<Order> search(String query) {
        List<Order> all = orderRepository.findAllByOrderByIdDesc();
        if (query == null || query.isBlank()) {
            return all;
        }
        String q = query.trim().toLowerCase();
        Map<Long, List<OrderMaterial>> materials = consumptionByOrder(all);
        List<Order> result = new ArrayList<>();
        for (Order order : all) {
            Customer c = order.getCustomer();
            StringBuilder haystack = new StringBuilder();
            haystack.append('№').append(order.getId()).append('\n');
            append(haystack, c.getName(), c.getPhoneNumber(), c.getEmail(), c.getCity(), c.getStreet());
            append(haystack, order.getProduct(), order.getDetails());
            for (OrderMaterial used : materials.getOrDefault(order.getId(), List.of())) {
                append(haystack, used.getMaterial().getName(), used.getMaterial().getSheetSize());
            }
            if (haystack.toString().toLowerCase().contains(q.startsWith("№") || !q.matches("\\d+") ? q : "№" + q)) {
                result.add(order);
            }
        }
        return result;
    }

    /** По одной сводке на каждого клиента из списка; у кого заказы были недавно — выше. */
    public List<CustomerSummary> summaries(List<Customer> customers) {
        Map<Long, List<Order>> byCustomer = new HashMap<>();
        for (Order order : orderRepository.findAllByOrderByIdDesc()) {
            byCustomer.computeIfAbsent(order.getCustomer().getId(), k -> new ArrayList<>()).add(order);
        }
        List<CustomerSummary> result = new ArrayList<>();
        for (Customer customer : customers) {
            List<Order> orders = byCustomer.getOrDefault(customer.getId(), List.of());
            BigDecimal total = orders.stream().map(Order::getTotalCost).reduce(BigDecimal.ZERO, BigDecimal::add);
            int active = (int) orders.stream().filter(o -> o.getStatus() != OrderStatus.DONE).count();
            LocalDateTime last = orders.isEmpty() ? null : orders.get(0).getCreatedAt();
            result.add(new CustomerSummary(customer, orders.size(), active, total, last));
        }
        result.sort(Comparator.comparing(CustomerSummary::getLastOrderAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    private static String describe(Cliche cl, LengthUnit lu) {
        String unit = " " + lu.getLabel();
        StringBuilder sb = new StringBuilder("Клише: ")
                .append(lu.format(cl.getWidthCm())).append(" × ").append(lu.format(cl.getLengthCm())).append(unit);
        if (cl.getMarginCm() != null && cl.getMarginCm().signum() > 0) {
            sb.append(", поля ").append(lu.format(cl.getMarginCm())).append(unit);
        }
        sb.append(", общая площадь с полями ").append(area(cl.getAreaCm2())).append(" см²")
                .append(", ").append(cl.getQuantity()).append(" шт.");
        if (cl.getMaterial() != null) {
            sb.append(", ").append(cl.getMaterial().getName());
        }
        return sb.append(" — ").append(money(cl.getTotalPrice())).toString();
    }

    private static String area(BigDecimal cm2) {
        return cm2.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static void append(StringBuilder sb, String... values) {
        for (String value : values) {
            if (value != null) {
                sb.append(value).append('\n');
            }
        }
    }

    private static LinkedHashSet<Long> unique(List<Long> ids) {
        return new LinkedHashSet<>(ids == null ? List.of() : ids);
    }

    private static void requireSameCustomer(Customer owner, Customer customer) {
        if (owner == null || !owner.getId().equals(customer.getId())) {
            throw new NotFoundException("Расчёт не найден");
        }
    }
}
