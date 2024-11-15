package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.repository.AccountRepository;
import com.springboot.printmastercrm.repository.CustomerRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class CustomerService implements UserDetailsService {

    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private AccountRepository accountRepository;

    public List<Customer> getCustomersByManager(String managerUsername) {
        return customerRepository.findByManagerUsername(managerUsername);
    }

    public Customer createCustomerForManager(Customer customer, String managerUsername) {
        customer.setManagerUsername(managerUsername);
        Account managerAccount = accountRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new RuntimeException("Manager account not found"));
        customer.setAccount(managerAccount);
        return customerRepository.save(customer);
    }

    public Customer findById(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    public Customer updateCustomer(Long id, Customer updatedCustomer) {
        Customer existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        existingCustomer.setAccount(updatedCustomer.getAccount());
        existingCustomer.setId(updatedCustomer.getId());

        return customerRepository.save(updatedCustomer);
    }

    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
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


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<Customer> byUsername = customerRepository.findByUsername(username);
        if (byUsername.isPresent()) {
            return (UserDetails) byUsername.get();
        }
        throw new UsernameNotFoundException(username);
    }
}