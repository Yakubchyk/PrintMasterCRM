package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.repository.CustomerRepository;
import jakarta.servlet.SessionTrackingMode;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class CustomerService implements UserDetailsService {

    @Autowired
    private CustomerRepository customerRepository;

    // Получение списка клиентов, созданных конкретным менеджером
    public List<Customer> getCustomersByManager(String managerUsername) {
        return customerRepository.findByManagerUsername(managerUsername);
    }

    // Создание клиента и привязка к менеджеру
    public Customer createCustomerForManager(Customer customer, String managerUsername) {
        customer.setManagerUsername(managerUsername);
        return customerRepository.save(customer);
    }


    public Customer existByUsername(String username) {
        Optional<Customer> customer = customerRepository.findByUsername(username);
        if (customer.isPresent()) {
            return customer.get();
        }
        return null;
    }


    public Customer createCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    public Customer updateCustomer(Long id, Customer updatedCustomer) {
        Customer existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        existingCustomer.setRole(updatedCustomer.getRole());
        existingCustomer.setAccount(updatedCustomer.getAccount());
        return customerRepository.save(existingCustomer);
    }

    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Customer> byUsername = customerRepository.findByUsername(username);
        if (byUsername.isPresent()) {
            return (UserDetails) byUsername.get();
        }
        throw new UsernameNotFoundException(username);
    }
}