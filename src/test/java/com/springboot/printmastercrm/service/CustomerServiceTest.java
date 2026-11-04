package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.ExistsException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.AccountRepository;
import com.springboot.printmastercrm.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AccountRepository accountRepository;
    @InjectMocks
    private CustomerService service;

    private Account anna;
    private Customer annasCustomer;

    @BeforeEach
    void setUp() {
        anna = new Account();
        anna.setId(1L);
        anna.setUsername("anna");
        annasCustomer = Customer.builder().id(10L).username("Firma A")
                .phoneNumber("111").email("a@firma.de").account(anna).build();
    }

    private Customer newCustomer(String name, String phone, String email) {
        return Customer.builder().username(name).phoneNumber(phone).email(email).build();
    }

    // ---- доступ к чужим клиентам ----

    @Test
    void ownerGetsCustomer() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(annasCustomer));

        assertThat(service.getOwnedCustomer(10L, "anna")).isSameAs(annasCustomer);
    }

    @Test
    void foreignCustomerLooksLikeMissing() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(annasCustomer));

        assertThatThrownBy(() -> service.getOwnedCustomer(10L, "boris"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void missingCustomerIsNotFound() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwnedCustomer(99L, "anna"))
                .isInstanceOf(NotFoundException.class);
    }

    // ---- уникальность на всю систему ----

    @Test
    void createRejectsDuplicatePhoneOfAnyManager() {
        when(accountRepository.findByUsername("anna")).thenReturn(Optional.of(anna));
        when(customerRepository.existsByPhoneNumber("111")).thenReturn(true);

        assertThatThrownBy(() -> service.createCustomerForManager(newCustomer("Firma C", "111", "c@firma.de"), "anna"))
                .isInstanceOf(ExistsException.class)
                .hasMessageContaining("телефон");
        verify(customerRepository, never()).save(any());
    }

    @Test
    void createRejectsDuplicateEmailAndName() {
        when(accountRepository.findByUsername("anna")).thenReturn(Optional.of(anna));
        when(customerRepository.existsByEmail("c@firma.de")).thenReturn(true);

        assertThatThrownBy(() -> service.createCustomerForManager(newCustomer("Firma C", "333", "c@firma.de"), "anna"))
                .isInstanceOf(ExistsException.class)
                .hasMessageContaining("email");

        when(customerRepository.existsByUsername("Firma A")).thenReturn(true);
        assertThatThrownBy(() -> service.createCustomerForManager(newCustomer("Firma A", "333", "x@firma.de"), "anna"))
                .isInstanceOf(ExistsException.class);
    }

    @Test
    void createSavesUniqueCustomerForManagerAndIgnoresSuppliedId() {
        when(accountRepository.findByUsername("anna")).thenReturn(Optional.of(anna));
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
        Customer input = newCustomer("  Firma C ", " 333 ", " c@firma.de ");
        input.setId(777L);

        Customer saved = service.createCustomerForManager(input, "anna");

        assertThat(saved.getId()).isNull();
        assertThat(saved.getUsername()).isEqualTo("Firma C");
        assertThat(saved.getPhoneNumber()).isEqualTo("333");
        assertThat(saved.getAccount()).isSameAs(anna);
    }

    @Test
    void createRejectsBlankFields() {
        when(accountRepository.findByUsername("anna")).thenReturn(Optional.of(anna));

        assertThatThrownBy(() -> service.createCustomerForManager(newCustomer("Firma C", "  ", "c@firma.de"), "anna"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updateRejectsPhoneOfAnotherCustomer() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(annasCustomer));
        when(customerRepository.existsByPhoneNumberAndIdNot("222", 10L)).thenReturn(true);
        Customer update = newCustomer("Firma A", "222", "a@firma.de");
        update.setId(10L);

        assertThatThrownBy(() -> service.updateCustomer(update, "anna"))
                .isInstanceOf(ExistsException.class);
        assertThat(annasCustomer.getPhoneNumber()).isEqualTo("111");
    }

    @Test
    void updateKeepsOwnerAndChangesOnlyCardFields() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(annasCustomer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(i -> i.getArgument(0));
        Account intruder = new Account();
        intruder.setUsername("boris");
        Customer update = newCustomer("Firma A neu", "222", "neu@firma.de");
        update.setId(10L);
        update.setAccount(intruder);

        Customer saved = service.updateCustomer(update, "anna");

        assertThat(saved.getUsername()).isEqualTo("Firma A neu");
        assertThat(saved.getPhoneNumber()).isEqualTo("222");
        assertThat(saved.getAccount()).isSameAs(anna);
    }

    @Test
    void foreignManagerCannotUpdateCustomer() {
        when(customerRepository.findById(10L)).thenReturn(Optional.of(annasCustomer));
        Customer update = newCustomer("Hack", "999", "h@x.de");
        update.setId(10L);

        assertThatThrownBy(() -> service.updateCustomer(update, "boris"))
                .isInstanceOf(NotFoundException.class);
        verify(customerRepository, never()).save(any());
    }
}
