package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Account;
import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.Order;
import com.springboot.printmastercrm.entity.OrderStatus;
import com.springboot.printmastercrm.entity.PostPress;
import com.springboot.printmastercrm.entity.Printing;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.OrderRepository;
import com.springboot.printmastercrm.repository.PostPressRepository;
import com.springboot.printmastercrm.repository.PrintingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private PostPressRepository postPressRepository;
    @Mock
    private PrintingRepository printingRepository;
    @InjectMocks
    private OrderService service;

    private Account manager;
    private Customer customer;
    private Customer otherCustomer;

    @BeforeEach
    void setUp() {
        manager = new Account();
        manager.setId(1L);
        manager.setUsername("anna");
        customer = Customer.builder().id(10L).username("Firma A").account(manager).build();
        otherCustomer = Customer.builder().id(20L).username("Firma B").account(new Account()).build();
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
    }

    private PostPress postPress(Long id, Customer owner, String price) {
        PostPress pp = new PostPress();
        pp.setId(id);
        pp.setCustomer(owner);
        pp.setTotalPrice(new BigDecimal(price));
        return pp;
    }

    private Printing printing(Long id, Customer owner, String cost) {
        Printing p = new Printing();
        p.setId(id);
        p.setCustomer(owner);
        p.setTotalCost(new BigDecimal(cost));
        return p;
    }

    @Test
    void sumsSelectedCalculationsOnTheServer() {
        when(postPressRepository.findById(1L)).thenReturn(Optional.of(postPress(1L, customer, "164.06")));
        when(printingRepository.findById(2L)).thenReturn(Optional.of(printing(2L, customer, "125.00")));

        Order order = service.createOrder(customer, manager, "  Визитки  ", List.of(1L), List.of(2L));

        assertThat(order.getPostpressPrice()).isEqualByComparingTo("164.06");
        assertThat(order.getTotalCost()).isEqualByComparingTo("289.06");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getProduct()).isEqualTo("Визитки");
        assertThat(order.getAccount()).isSameAs(manager);
    }

    @Test
    void sameCalculationSelectedTwiceCountsOnce() {
        when(postPressRepository.findById(1L)).thenReturn(Optional.of(postPress(1L, customer, "100.00")));

        Order order = service.createOrder(customer, manager, "x", List.of(1L, 1L), List.of());

        assertThat(order.getTotalCost()).isEqualByComparingTo("100.00");
    }

    @Test
    void calculationOfAnotherCustomerIsRejectedAsNotFound() {
        when(postPressRepository.findById(1L)).thenReturn(Optional.of(postPress(1L, otherCustomer, "100.00")));

        assertThatThrownBy(() -> service.createOrder(customer, manager, "x", List.of(1L), List.of()))
                .isInstanceOf(NotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void unknownCalculationIsNotFound() {
        when(printingRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(customer, manager, "x", List.of(), List.of(5L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void requiresDescriptionAndAtLeastOneCalculation() {
        assertThatThrownBy(() -> service.createOrder(customer, manager, "  ", List.of(1L), List.of()))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.createOrder(customer, manager, "x", List.of(), List.of()))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.createOrder(customer, manager, "x", null, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    private Order orderOf(String managerUsername) {
        Account owner = new Account();
        owner.setUsername(managerUsername);
        Order o = new Order();
        o.setAccount(owner);
        o.setStatus(OrderStatus.NEW);
        return o;
    }

    @Test
    void managerChangesStatusOfOwnOrder() {
        Order o = orderOf("anna");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(o));

        service.setStatus(7L, OrderStatus.READY, "anna", false);

        assertThat(o.getStatus()).isEqualTo(OrderStatus.READY);
        verify(orderRepository).save(o);
    }

    @Test
    void managerCannotChangeForeignOrder() {
        Order o = orderOf("boris");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(o));

        assertThatThrownBy(() -> service.setStatus(7L, OrderStatus.READY, "anna", false))
                .isInstanceOf(NotFoundException.class);
        assertThat(o.getStatus()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void adminChangesAnyOrder() {
        Order o = orderOf("boris");
        when(orderRepository.findById(7L)).thenReturn(Optional.of(o));

        service.setStatus(7L, OrderStatus.SHIPPED, "admin", true);

        assertThat(o.getStatus()).isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void visibleOrdersAreScopedByRole() {
        service.getVisibleOrders("anna", false, null);
        verify(orderRepository).findByAccountUsername("anna");

        service.getVisibleOrders("anna", false, OrderStatus.NEW);
        verify(orderRepository).findByAccountUsernameAndStatus("anna", OrderStatus.NEW);

        service.getVisibleOrders("admin", true, null);
        verify(orderRepository).findAll();

        service.getVisibleOrders("admin", true, OrderStatus.READY);
        verify(orderRepository).findByStatus(OrderStatus.READY);
    }
}
