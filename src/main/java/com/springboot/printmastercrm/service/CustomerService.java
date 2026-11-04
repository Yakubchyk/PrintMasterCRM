package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.ExistsException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.ClicheRepository;
import com.springboot.printmastercrm.repository.CustomerRepository;
import com.springboot.printmastercrm.repository.PostPressRepository;
import com.springboot.printmastercrm.repository.PrintingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PostPressRepository postPressRepository;
    private final PrintingRepository printingRepository;
    private final ClicheRepository clicheRepository;
    private final OrderService orderService;

    public List<Customer> search(String query) {
        if (query == null || query.isBlank()) {
            return customerRepository.findAllByOrderByNameAsc();
        }
        String q = query.trim();
        return customerRepository
                .findByNameContainingIgnoreCaseOrPhoneNumberContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByNameAsc(q, q, q);
    }

    public Customer get(Long id) {
        return customerRepository.findById(id).orElseThrow(() -> new NotFoundException("Клиент не найден"));
    }

    @Transactional
    public Customer create(String name, String phone, String email) {
        Customer customer = Customer.builder()
                .name(trim(name)).phoneNumber(trim(phone)).email(trim(email)).build();
        requireFilled(customer);
        assertUnique(customer, null);
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer update(Long id, String name, String phone, String email) {
        Customer existing = get(id);
        Customer incoming = Customer.builder()
                .name(trim(name)).phoneNumber(trim(phone)).email(trim(email)).build();
        requireFilled(incoming);
        assertUnique(incoming, id);
        existing.setName(incoming.getName());
        existing.setPhoneNumber(incoming.getPhoneNumber());
        existing.setEmail(incoming.getEmail());
        return customerRepository.save(existing);
    }

    /** Удаляет клиента вместе с его заказами и расчётами. */
    @Transactional
    public void delete(Long id) {
        Customer customer = get(id);
        orderService.deleteByCustomer(id);
        postPressRepository.deleteByCustomerId(id);
        printingRepository.deleteByCustomerId(id);
        clicheRepository.deleteByCustomerId(id);
        customerRepository.delete(customer);
    }

    private void assertUnique(Customer customer, Long excludeId) {
        boolean creating = excludeId == null;
        if (creating ? customerRepository.existsByName(customer.getName())
                : customerRepository.existsByNameAndIdNot(customer.getName(), excludeId)) {
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

    private static void requireFilled(Customer customer) {
        if (customer.getName().isEmpty() || customer.getPhoneNumber().isEmpty() || customer.getEmail().isEmpty()) {
            throw new BusinessRuleException("Заполните название организации, телефон и email");
        }
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
