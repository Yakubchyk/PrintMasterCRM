package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.Order;
import com.springboot.printmastercrm.entity.OrderStatus;
import com.springboot.printmastercrm.entity.PostPress;
import com.springboot.printmastercrm.entity.Printing;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.OrderRepository;
import com.springboot.printmastercrm.repository.PostPressRepository;
import com.springboot.printmastercrm.repository.PrintingRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PostPressRepository postPressRepository;

    @Autowired
    private PrintingRepository printingRepository;

    /**
     * Оформляет заказ из выбранных расчётов клиента. Суммы считаются на сервере
     * по сохранённым расчётам, из запроса принимаются только id расчётов и описание.
     * Чужой или несуществующий расчёт даёт NotFoundException.
     */
    @Transactional
    public Order createOrder(Customer customer, Account manager, String product,
                             List<Long> postPressIds, List<Long> printingIds) {
        if (product == null || product.isBlank()) {
            throw new BusinessRuleException("Укажите детали заказа");
        }
        LinkedHashSet<Long> ppIds = new LinkedHashSet<>(postPressIds == null ? List.of() : postPressIds);
        LinkedHashSet<Long> prIds = new LinkedHashSet<>(printingIds == null ? List.of() : printingIds);
        if (ppIds.isEmpty() && prIds.isEmpty()) {
            throw new BusinessRuleException("Выберите хотя бы один расчёт для заказа");
        }

        BigDecimal postpressTotal = BigDecimal.ZERO;
        for (Long id : ppIds) {
            PostPress pp = postPressRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("PostPress not found"));
            if (pp.getCustomer() == null || !pp.getCustomer().getId().equals(customer.getId())) {
                throw new NotFoundException("PostPress not found");
            }
            if (pp.getTotalPrice() != null) {
                postpressTotal = postpressTotal.add(pp.getTotalPrice());
            }
        }

        BigDecimal printingTotal = BigDecimal.ZERO;
        for (Long id : prIds) {
            Printing pr = printingRepository.findById(id)
                    .orElseThrow(() -> new NotFoundException("Printing not found"));
            if (pr.getCustomer() == null || !pr.getCustomer().getId().equals(customer.getId())) {
                throw new NotFoundException("Printing not found");
            }
            printingTotal = printingTotal.add(pr.getTotalCost());
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setAccount(manager);
        order.setProduct(product.trim());
        order.setPostpressPrice(postpressTotal.setScale(2, RoundingMode.HALF_UP));
        order.setTotalCost(postpressTotal.add(printingTotal).setScale(2, RoundingMode.HALF_UP));
        order.setStatus(OrderStatus.NEW);
        return orderRepository.save(order);
    }

    /**
     * Меняет статус заказа. Менеджер может менять только свои заказы, админ любые.
     * Чужой заказ неотличим от несуществующего (NotFoundException).
     */
    public void setStatus(Long orderId, OrderStatus status, String username, boolean admin) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (!admin && !order.getAccount().getUsername().equals(username)) {
            throw new NotFoundException("Order not found");
        }
        order.setStatus(status);
        orderRepository.save(order);
    }

    /**
     * Заказы, которые пользователь вправе видеть: админ все, менеджер только свои.
     * status == null означает «любой статус».
     */
    public List<Order> getVisibleOrders(String username, boolean admin, OrderStatus status) {
        if (admin) {
            return status == null ? orderRepository.findAll() : orderRepository.findByStatus(status);
        }
        return status == null
                ? orderRepository.findByAccountUsername(username)
                : orderRepository.findByAccountUsernameAndStatus(username, status);
    }
}
