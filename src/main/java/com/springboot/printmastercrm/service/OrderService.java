package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.*;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.ClicheRepository;
import com.springboot.printmastercrm.repository.OrderRepository;
import com.springboot.printmastercrm.repository.PostPressRepository;
import com.springboot.printmastercrm.repository.PrintingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PostPressRepository postPressRepository;
    private final PrintingRepository printingRepository;
    private final ClicheRepository clicheRepository;

    /**
     * Оформляет заказ из выбранных расчётов клиента. Суммы берутся из сохранённых расчётов,
     * из запроса принимаются только их id и описание. Расчёт другого клиента даёт NotFoundException.
     */
    @Transactional
    public Order createOrder(Customer customer, String product,
                             List<Long> postPressIds, List<Long> printingIds, List<Long> clicheIds) {
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
        return orderRepository.save(order);
    }

    @Transactional
    public void setStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Заказ не найден"));
        order.setStatus(status);
        orderRepository.save(order);
    }

    @Transactional
    public void delete(Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new NotFoundException("Заказ не найден");
        }
        orderRepository.deleteById(orderId);
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
