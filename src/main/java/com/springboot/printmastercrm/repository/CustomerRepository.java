package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByAccountUsername(String username);

    boolean existsByAccountId(Long accountId);

    boolean existsByUsername(String username);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

}
