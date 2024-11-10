package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByManagerId(Long managerId);
    Optional<Client> findByUserId(Long userId);
}