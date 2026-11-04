package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.Order;
import com.springboot.printmastercrm.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findAllByOrderByIdDesc();

    List<Order> findByStatusOrderByIdDesc(OrderStatus status);

    List<Order> findByCustomerIdOrderByIdDesc(Long customerId);

    List<Order> findByCustomerIdAndStatusOrderByIdDesc(Long customerId, OrderStatus status);

    void deleteByCustomerId(Long customerId);
}
