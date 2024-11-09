package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.Stamping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StampingRepository extends JpaRepository<Stamping, Long> {
    List<Stamping> findByOrderId(Long orderId);
}