package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.*;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.ClicheRepository;
import com.springboot.printmastercrm.repository.OrderMaterialRepository;
import com.springboot.printmastercrm.repository.OrderRepository;
import com.springboot.printmastercrm.repository.PostPressRepository;
import com.springboot.printmastercrm.repository.PrintingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    /**
     * Оформляет заказ из выбранных расчётов клиента. Суммы берутся из сохранённых расчётов,
     * из запроса принимаются только их id и описание. Расчёт другого клиента даёт NotFoundException.
     * Материалы из consumption (id материала -> количество) списываются со склада; если какого-то
     * не хватает, заказ не создаётся.
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

        BigDecimal postpress = BigDecimal.ZERO;
        for (Long id : ppIds) {
            PostPress pp = postPressRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Расчёт тиснения не найден"));
            requireSameCustomer(pp.getCustomer(), customer);
            postpress = postpress.add(pp.getTotalPrice());
        }
        BigDecimal printing = BigDecimal.ZERO;
        for (Long id : prIds) {
            Printing pr = printingRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Расчёт печати не найден"));
            requireSameCustomer(pr.getCustomer(), customer);
            printing = printing.add(pr.getTotalCost());
        }
        BigDecimal cliche = BigDecimal.ZERO;
        for (Long id : clIds) {
            Cliche cl = clicheRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Расчёт клише не найден"));
            requireSameCustomer(cl.getCustomer(), customer);
            cliche = cliche.add(cl.getTotalPrice());
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setProduct(product.trim());
        order.setPostpressPrice(postpress.setScale(2, RoundingMode.HALF_UP));
        order.setPrintingPrice(printing.setScale(2, RoundingMode.HALF_UP));
        order.setClichePrice(cliche.setScale(2, RoundingMode.HALF_UP));
        order.setTotalCost(postpress.add(printing).add(cliche).setScale(2, RoundingMode.HALF_UP));
        order.setStatus(OrderStatus.NEW);
        order = orderRepository.save(order);

        if (consumption != null) {
            for (Map.Entry<Long, BigDecimal> item : consumption.entrySet()) {
                BigDecimal quantity = item.getValue();
                if (quantity == null || quantity.signum() == 0) {
                    continue;
                }
                materialService.consume(item.getKey(), quantity, "Заказ №" + order.getId());
                OrderMaterial used = new OrderMaterial();
                used.setOrder(order);
                used.setMaterial(materialService.get(item.getKey()));
                used.setQuantity(quantity);
                orderMaterialRepository.save(used);
            }
        }
        return order;
    }

    @Transactional
    public void setStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден"));
        order.setStatus(status);
        orderRepository.save(order);
    }

    /** Удаляет заказ; списанные на него материалы возвращаются на склад. */
    @Transactional
    public void delete(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден"));
        removeWithStockReturn(order);
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
            materialService.receive(used.getMaterial().getId(), used.getQuantity(), "Возврат: заказ №" + order.getId() + " удалён");
            orderMaterialRepository.delete(used);
        }
        orderRepository.delete(order);
    }

    /** Списанные материалы по заказам: id заказа -> список. */
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

    private static LinkedHashSet<Long> unique(List<Long> ids) {
        return new LinkedHashSet<>(ids == null ? List.of() : ids);
    }

    private static void requireSameCustomer(Customer owner, Customer customer) {
        if (owner == null || !owner.getId().equals(customer.getId())) {
            throw new NotFoundException("Расчёт не найден");
        }
    }
}
