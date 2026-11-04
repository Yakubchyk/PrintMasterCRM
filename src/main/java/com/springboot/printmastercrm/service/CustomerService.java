package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.Order;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.ExistsException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class CustomerService {

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
        Account managerAccount = accountRepository.findByUsername(managerUsername)
                .orElseThrow(() -> new NotFoundException("Manager account not found"));
        customer.setId(null); // всегда создаём нового клиента, а не обновляем чужого
        normalizeAndValidate(customer);
        assertUnique(customer, null);
        customer.setManagerUsername(managerUsername);
        customer.setAccount(managerAccount);
        return customerRepository.save(customer);
    }

    // Поиск клиента по ID (без проверки владельца: для админских операций)
    public Customer findById(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new NotFoundException("Customer not found"));
    }

    /**
     * Возвращает клиента, только если он закреплён за указанным менеджером.
     * Чужой и несуществующий клиент неразличимы: в обоих случаях NotFoundException (404),
     * чтобы нельзя было перебором выяснить, какие id существуют.
     */
    public Customer getOwnedCustomer(Long customerId, String managerUsername) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));
        if (customer.getAccount() == null || !customer.getAccount().getUsername().equals(managerUsername)) {
            throw new NotFoundException("Customer not found");
        }
        return customer;
    }

    /**
     * Обновляет только данные карточки клиента (название, телефон, email).
     * Менеджер и аккаунт из формы не принимаются: их меняет только админ через передачу клиента.
     */
    public Customer updateCustomer(Customer updatedCustomer, String managerUsername) {
        Customer existingCustomer = getOwnedCustomer(updatedCustomer.getId(), managerUsername);

        normalizeAndValidate(updatedCustomer);
        assertUnique(updatedCustomer, existingCustomer.getId());

        existingCustomer.setUsername(updatedCustomer.getUsername());
        existingCustomer.setPhoneNumber(updatedCustomer.getPhoneNumber());
        existingCustomer.setEmail(updatedCustomer.getEmail());

        return customerRepository.save(existingCustomer);
    }

    /**
     * Удаляет клиента вместе со всеми связанными данными.
     * Порядок важен: сначала всё, что ссылается на клиента по внешнему ключу.
     */
    public void deleteCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        orderRepository.deleteAll(orderRepository.findByCustomerId(customerId));
        postPressRepository.deleteByCustomerId(customerId);
        printingRepository.deleteByCustomerId(customerId);

        customerRepository.delete(customer);
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
                .orElseThrow(() -> new NotFoundException("Customer not found"));

        Account newManager = accountRepository.findById(newManagerId)
                .orElseThrow(() -> new NotFoundException("Manager not found"));

        customer.setManagerUsername(newManager.getUsername());
        customer.setAccount(newManager);
        customerRepository.save(customer);

        List<Order> orders = orderRepository.findByCustomerId(customerId);
        for (Order order : orders) {
            order.setAccount(newManager); // Устанавливаем нового менеджера
            orderRepository.save(order);
        }
    }

    private void normalizeAndValidate(Customer customer) {
        customer.setUsername(trimToEmpty(customer.getUsername()));
        customer.setPhoneNumber(trimToEmpty(customer.getPhoneNumber()));
        customer.setEmail(trimToEmpty(customer.getEmail()));
        if (customer.getUsername().isEmpty() || customer.getPhoneNumber().isEmpty() || customer.getEmail().isEmpty()) {
            throw new BusinessRuleException("Заполните название организации, телефон и email");
        }
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    /** excludeId == null: создание, иначе обновление (саму запись не считаем дубликатом). */
    private void assertUnique(Customer customer, Long excludeId) {
        boolean creating = excludeId == null;
        if (creating ? customerRepository.existsByUsername(customer.getUsername())
                : customerRepository.existsByUsernameAndIdNot(customer.getUsername(), excludeId)) {
            throw new ExistsException("Клиент с таким названием уже существует");
        }
        if (creating ? customerRepository.existsByPhoneNumber(customer.getPhoneNumber())
                : customerRepository.existsByPhoneNumberAndIdNot(customer.getPhoneNumber(), excludeId)) {
            throw new ExistsException("Клиент с таким телефоном уже существует");
        }
        if (creating ? customerRepository.existsByEmail(customer.getEmail())
                : customerRepository.existsByEmailAndIdNot(customer.getEmail(), excludeId)) {
            throw new ExistsException("Клиент с таким email уже существует");
        }
    }
}
