package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Client;
import com.springboot.printmastercrm.entity.Order;
import com.springboot.printmastercrm.entity.User;
import com.springboot.printmastercrm.repository.ClientRepository;
import com.springboot.printmastercrm.repository.OrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.springboot.printmastercrm.entity.User.Role.*;

@Service
@Transactional
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ClientRepository clientRepository;

    public List<Order> getOrdersByUser(User user) {
        if (user.getRoles().equals(ROLE_ADMIN)) {
            return orderRepository.findAll();
        } else if (user.getRoles().equals(ROLE_MANAGER)) {
            return orderRepository.findByManagerId(user.getId());
        } else if (user.getRoles().equals(ROLE_USER)) {
            Client client = clientRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new RuntimeException("Client not found for user"));
            return orderRepository.findByClientId(client.getId());
        }
        throw new RuntimeException("Invalid role");
    }

    public Order createOrder(Order order, User user) {
        order.setManager(user);
        Client client = clientRepository.findById(order.getClient().getId())
                .orElseThrow(() -> new RuntimeException("Client not found"));
        order.setClient(client);
        return orderRepository.save(order);
    }


    public Order updateOrder(Order updatedOrder) {

        return orderRepository.save(updatedOrder);
    }

    public void deleteOrder(Order order) {

        orderRepository.delete(order);
    }
}