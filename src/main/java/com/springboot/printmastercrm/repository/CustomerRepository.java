package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findAllByOrderByNameAsc();

    List<Customer> findByNameContainingIgnoreCaseOrPhoneNumberContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(
            String name, String phone, String email);

    boolean existsByName(String name);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByNameAndIdNot(String name, Long id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);
}
