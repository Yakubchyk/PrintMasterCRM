package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findById(Long id);

    Optional<Customer> findByUsername(String username);

    List<Customer> findByManagerUsername(String managerUsername);

}
