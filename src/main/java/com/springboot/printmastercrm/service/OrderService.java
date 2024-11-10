package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Client;
import com.springboot.printmastercrm.entity.Order;
import com.springboot.printmastercrm.entity.User;
import com.springboot.printmastercrm.repository.ClientRepository;
import com.springboot.printmastercrm.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ClientRepository clientRepository;

    public List<Order> getOrdersByUser(User user) {
        switch (user.getRole()) {
            case ADMIN:
                return orderRepository.findAll();
            case MANAGER:
                return orderRepository.findByManagerId(user.getId());
            case USER:
                Client client = clientRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new RuntimeException("Client not found for user"));
                return orderRepository.findByClientId(client.getId());
            default:
                throw new RuntimeException("Invalid role");
        }
    }

    public Order createOrder(Order order, User user) {
        if (user.getRole() == User.Role.ADMIN || user.getRole() == User.Role.MANAGER) {
            order.setManager(user);
            return orderRepository.save(order);
        } else {
            throw new RuntimeException("Only managers and admins can create orders");
        }
    }

    public Order getOrderById(Long id, User user) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (user.getRole() == User.Role.ADMIN ||
                (user.getRole() == User.Role.MANAGER && order.getManager().getId().equals(user.getId())) ||
                (user.getRole() == User.Role.USER && order.getClient().getManager().getId().equals(user.getId()))) {
            return order;
        }
        throw new RuntimeException("Unauthorized access to order");
    }

    public Order updateOrder(Long id, Order updatedOrder, User user) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (user.getRole() != User.Role.ADMIN && !existingOrder.getManager().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to update order");
        }
        //existingOrder.setProduct(updatedOrder.getProduct());
        existingOrder.setPrintDetails(updatedOrder.getPrintDetails());
        existingOrder.setPostPressDetails(updatedOrder.getPostPressDetails());
        existingOrder.setTotalCost(updatedOrder.getTotalCost());
        return orderRepository.save(existingOrder);
    }

    public void deleteOrder(Long id, User user) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        if (user.getRole() != User.Role.ADMIN && !order.getManager().getId().equals(user.getId())) {
            throw new RuntimeException("Unauthorized access to delete order");
        }
        orderRepository.delete(order);
    }
}