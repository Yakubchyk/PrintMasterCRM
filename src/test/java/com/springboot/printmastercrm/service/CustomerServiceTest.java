package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.ExistsException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PostPressRepository postPressRepository;
    @Mock
    private PrintingRepository printingRepository;
    @Mock
    private ClicheRepository clicheRepository;
    @Mock
    private OrderService orderService;
    @InjectMocks
    private CustomerService service;

    @Test
    void createTrimsAndSavesUniqueCustomer() {
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

        Customer saved = service.create("  Firma C ", " 333 ", " c@firma.de ");

        assertThat(saved.getName()).isEqualTo("Firma C");
        assertThat(saved.getPhoneNumber()).isEqualTo("333");
        assertThat(saved.getEmail()).isEqualTo("c@firma.de");
    }

    @Test
    void createRejectsDuplicatePhone() {
        when(customerRepository.existsByPhoneNumber("111")).thenReturn(true);

        assertThatThrownBy(() -> service.create("Firma C", "111", "c@firma.de"))
                .isInstanceOf(ExistsException.class)
                .hasMessageContaining("телефон");
        verify(customerRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateNameAndEmail() {
        when(customerRepository.existsByName("Firma A")).thenReturn(true);
        assertThatThrownBy(() -> service.create("Firma A", "333", "x@firma.de")).isInstanceOf(ExistsException.class);

        when(customerRepository.existsByEmail("a@firma.de")).thenReturn(true);
        assertThatThrownBy(() -> service.create("Firma C", "333", "a@firma.de"))
                .isInstanceOf(ExistsException.class)
                .hasMessageContaining("email");
    }

    @Test
    void createRejectsBlankFields() {
        assertThatThrownBy(() -> service.create("Firma C", "  ", "c@firma.de")).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.create(null, "1", "c@firma.de")).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateRejectsPhoneOfAnotherCustomer() {
        Customer existing = Customer.builder().id(10L).name("Firma A").phoneNumber("111").email("a@firma.de").build();
        when(customerRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(customerRepository.existsByPhoneNumberAndIdNot("222", 10L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(10L, "Firma A", "222", "a@firma.de"))
                .isInstanceOf(ExistsException.class);
        assertThat(existing.getPhoneNumber()).isEqualTo("111");
    }

    @Test
    void updateChangesCardFields() {
        Customer existing = Customer.builder().id(10L).name("Firma A").phoneNumber("111").email("a@firma.de").build();
        when(customerRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));

        Customer saved = service.update(10L, "Firma A neu", "222", "neu@firma.de");

        assertThat(saved.getName()).isEqualTo("Firma A neu");
        assertThat(saved.getPhoneNumber()).isEqualTo("222");
        assertThat(saved.getEmail()).isEqualTo("neu@firma.de");
    }

    @Test
    void missingCustomerIsNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRemovesOrdersAndCalculationsBeforeCustomer() {
        Customer existing = Customer.builder().id(10L).build();
        when(customerRepository.findById(10L)).thenReturn(Optional.of(existing));

        service.delete(10L);

        InOrder order = inOrder(orderService, postPressRepository, printingRepository, clicheRepository, customerRepository);
        order.verify(orderService).deleteByCustomer(10L);
        order.verify(postPressRepository).deleteByCustomerId(10L);
        order.verify(printingRepository).deleteByCustomerId(10L);
        order.verify(clicheRepository).deleteByCustomerId(10L);
        order.verify(customerRepository).delete(existing);
    }

    @Test
    void searchWithoutQueryReturnsEveryone() {
        service.search("  ");
        verify(customerRepository).findAllByOrderByNameAsc();
    }
}
