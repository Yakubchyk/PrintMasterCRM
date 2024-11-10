package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByManagerId(Long managerId);
    List<Order> findByClientId(Long clientId);
}