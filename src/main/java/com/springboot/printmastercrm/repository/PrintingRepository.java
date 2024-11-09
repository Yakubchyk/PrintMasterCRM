package com.springboot.printmastercrm.repository;

import com.springboot.postmastercrm.entity.Printing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrintingRepository extends JpaRepository<Printing, Long> {
    List<Printing> findByOrderId(Long orderId);
}