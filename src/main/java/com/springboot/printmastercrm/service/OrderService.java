package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Order;
import com.springboot.printmastercrm.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;


    public void saveOrder(Order order) {
        orderRepository.save(order);
    }
}
