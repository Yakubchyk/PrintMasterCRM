package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.Order;
import com.springboot.printmastercrm.repository.*;
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

    @Autowired
    private PostPressRepository postPressRepository;

    @Autowired
    private PrintingRepository printingRepository;

    @Autowired
    private OrderRepository orderRepository;

    // Получение клиентов по менеджеру
    public List<Customer> getCustomersByManager(String managerUsername) {
        return customerRepository.findByManagerUsername(managerUsername);
    }

    // Создание клиента для менеджера
    public Customer createCustomerForManager(Customer customer, String managerUsername) {
        customer.setManagerUsername(managerUsername);
        Account managerAccount = accountRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new RuntimeException("Manager account not found"));
        customer.setAccount(managerAccount);
        return customerRepository.save(customer);
    }

    // Поиск клиента по ID
    public Customer findById(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    public Customer updateCustomer(Customer updatedCustomer) {
        // Поиск существующего клиента
        Customer existingCustomer = customerRepository.findById(updatedCustomer.getId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        // Обновление полей клиента
        existingCustomer.setUsername(updatedCustomer.getUsername());
        existingCustomer.setPhoneNumber(updatedCustomer.getPhoneNumber());
        existingCustomer.setEmail(updatedCustomer.getEmail());
        existingCustomer.setManagerUsername(updatedCustomer.getManagerUsername());
        existingCustomer.setAccount(updatedCustomer.getAccount());

        // Сохранение обновлённого клиента
        return customerRepository.save(existingCustomer);
    }

    // Удаление клиента и связанных данных
    public void deleteCustomer(Long customerId) {
        // Удаление заказов клиента
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        for (Order order : orders) {
            orderRepository.delete(order);
        }

//        // Удаление связанных данных (PostPress и Printing)
//        postPressRepository.deleteAllByCustomerId(customerId);
//        printingRepository.deleteAllByCustomerId(customerId);

        // Удаление клиента
        customerRepository.deleteById(customerId);
    }

    // Получение всех клиентов
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    // Получение заказов клиента
    public List<Order> findOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    // Обновление заказа
    public void updateOrder(Order order) {
        orderRepository.save(order);
    }

    public void transferCustomerToManager(Long customerId, Long newManagerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        Account newManager = accountRepository.findById(newManagerId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        customer.setManagerUsername(newManager.getUsername());
        customer.setAccount(newManager);
        customerRepository.save(customer);

        List<Order> orders = orderRepository.findByCustomerId(customerId);
        for (Order order : orders) {
            order.setAccount(newManager); // Устанавливаем нового менеджера
            orderRepository.save(order);
        }
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