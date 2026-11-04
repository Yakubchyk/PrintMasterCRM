package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.Cliche;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClicheRepository extends JpaRepository<Cliche, Long> {

    List<Cliche> findByCustomerIdOrderByIdDesc(Long customerId);

    void deleteByCustomerId(Long customerId);
}
