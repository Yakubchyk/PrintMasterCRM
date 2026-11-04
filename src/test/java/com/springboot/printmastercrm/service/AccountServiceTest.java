package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.AccountRepository;
import com.springboot.printmastercrm.repository.CustomerRepository;
import com.springboot.printmastercrm.repository.OrderRepository;
import com.springboot.printmastercrm.repository.PostPressRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PostPressRepository postPressRepository;
    @InjectMocks
    private AccountService service;

    private Account boris;

    @BeforeEach
    void setUp() {
        boris = new Account();
        boris.setId(2L);
        boris.setUsername("boris");
    }

    @Test
    void cannotDeleteYourself() {
        when(accountRepository.findById(2L)).thenReturn(Optional.of(boris));

        assertThatThrownBy(() -> service.deleteManagerById(2L, "boris"))
                .isInstanceOf(BusinessRuleException.class);
        verify(accountRepository, never()).delete(boris);
    }

    @Test
    void cannotDeleteManagerWithCustomers() {
        when(accountRepository.findById(2L)).thenReturn(Optional.of(boris));
        when(customerRepository.existsByAccountId(2L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteManagerById(2L, "admin"))
                .isInstanceOf(BusinessRuleException.class);
        verify(accountRepository, never()).delete(boris);
    }

    @Test
    void cannotDeleteManagerWithOrders() {
        when(accountRepository.findById(2L)).thenReturn(Optional.of(boris));
        when(orderRepository.existsByAccountId(2L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteManagerById(2L, "admin"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void cannotDeleteManagerWithPostPressCalculations() {
        when(accountRepository.findById(2L)).thenReturn(Optional.of(boris));
        when(postPressRepository.existsByManagerId(2L)).thenReturn(true);

        assertThatThrownBy(() -> service.deleteManagerById(2L, "admin"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deletesManagerWithoutDependencies() {
        when(accountRepository.findById(2L)).thenReturn(Optional.of(boris));

        service.deleteManagerById(2L, "admin");

        verify(accountRepository).delete(boris);
    }

    @Test
    void unknownManagerIsNotFound() {
        when(accountRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteManagerById(5L, "admin"))
                .isInstanceOf(NotFoundException.class);
    }
}
