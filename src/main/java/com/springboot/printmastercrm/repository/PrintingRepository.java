package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.PostPress;
import com.springboot.printmastercrm.entity.Printing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrintingRepository extends JpaRepository<Printing, Long> {
    List<Printing> findByCustomerId(Long customerId);
}
