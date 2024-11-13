package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.entity.Order;
import com.springboot.printmastercrm.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    public List<Order> getOrdersByAccount(Account account) {
            return orderRepository.findOrderById(account.getId());

    }

    public Order createOrder(Order order, Account account) {
        return orderRepository.save(order);
    }


    public Order updateOrder(Order updatedOrder) {
        return orderRepository.save(updatedOrder);
    }

    public void deleteOrder(Order order) {
        orderRepository.delete(order);
    }
}