package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.*;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.exception.NotFoundException;
import com.springboot.printmastercrm.repository.ClicheRepository;
import com.springboot.printmastercrm.repository.OrderMaterialRepository;
import com.springboot.printmastercrm.repository.OrderRepository;
import com.springboot.printmastercrm.repository.PostPressRepository;
import com.springboot.printmastercrm.repository.PrintingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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
    @Mock
    private ClicheRepository clicheRepository;
    @Mock
    private OrderMaterialRepository orderMaterialRepository;
    @Mock
    private MaterialService materialService;
    @Mock
    private SettingService settingService;
    @InjectMocks
    private OrderService service;

    private Customer customer;
    private Customer other;

    @BeforeEach
    void setUp() {
        customer = Customer.builder().id(10L).name("Firma A").build();
        other = Customer.builder().id(20L).name("Firma B").build();
        lenient().when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(settingService.getSettings()).thenReturn(TestSettings.standard());
    }

    private PostPress postPress(Customer owner, String price) {
        PostPress pp = new PostPress();
        pp.setCustomer(owner);
        pp.setTotalPrice(new BigDecimal(price));
        return pp;
    }

    private Printing printing(Customer owner, String cost) {
        Printing p = new Printing();
        p.setCustomer(owner);
        p.setTotalCost(new BigDecimal(cost));
        return p;
    }

    private Cliche cliche(Customer owner, String price) {
        Cliche c = new Cliche();
        c.setCustomer(owner);
        c.setWidthCm(new BigDecimal("4"));
        c.setLengthCm(new BigDecimal("5"));
        c.setQuantity(1);
        c.setTotalPrice(new BigDecimal(price));
        return c;
    }

    @Test
    void sumsAllThreeKindsOfCalculationsOnTheServer() {
        when(postPressRepository.findById(1L)).thenReturn(Optional.of(postPress(customer, "164.06")));
        when(printingRepository.findById(2L)).thenReturn(Optional.of(printing(customer, "125.00")));
        when(clicheRepository.findById(3L)).thenReturn(Optional.of(cliche(customer, "75.00")));

        Order order = service.createOrder(customer, "  Визитки  ", List.of(1L), List.of(2L), List.of(3L), null);

        assertThat(order.getPostpressPrice()).isEqualByComparingTo("164.06");
        assertThat(order.getPrintingPrice()).isEqualByComparingTo("125.00");
        assertThat(order.getClichePrice()).isEqualByComparingTo("75.00");
        assertThat(order.getTotalCost()).isEqualByComparingTo("364.06");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getProduct()).isEqualTo("Визитки");
    }

    @Test
    void sameCalculationSelectedTwiceCountsOnce() {
        when(postPressRepository.findById(1L)).thenReturn(Optional.of(postPress(customer, "100.00")));

        Order order = service.createOrder(customer, "x", List.of(1L, 1L), null, null, null);

        assertThat(order.getTotalCost()).isEqualByComparingTo("100.00");
    }

    @Test
    void calculationOfAnotherCustomerIsRejected() {
        when(clicheRepository.findById(3L)).thenReturn(Optional.of(cliche(other, "75.00")));

        assertThatThrownBy(() -> service.createOrder(customer, "x", null, null, List.of(3L), null))
                .isInstanceOf(NotFoundException.class);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void unknownCalculationIsNotFound() {
        when(printingRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(customer, "x", null, List.of(5L), null, null))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void requiresDescriptionAndAtLeastOneCalculation() {
        assertThatThrownBy(() -> service.createOrder(customer, "  ", List.of(1L), null, null, null))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.createOrder(customer, "x", null, null, null, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void setStatusChangesOrder() {
        Order order = new Order();
        when(orderRepository.findById(7L)).thenReturn(Optional.of(order));

        service.setStatus(7L, OrderStatus.READY);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.READY);
        verify(orderRepository).save(order);
    }

    @Test
    void setStatusOfMissingOrderIsNotFound() {
        when(orderRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setStatus(7L, OrderStatus.READY)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void findChoosesQueryByFilters() {
        service.find(null, null);
        verify(orderRepository).findAllByOrderByIdDesc();

        service.find(OrderStatus.NEW, null);
        verify(orderRepository).findByStatusOrderByIdDesc(OrderStatus.NEW);

        service.find(null, 10L);
        verify(orderRepository).findByCustomerIdOrderByIdDesc(10L);

        service.find(OrderStatus.READY, 10L);
        verify(orderRepository).findByCustomerIdAndStatusOrderByIdDesc(10L, OrderStatus.READY);
    }

    @Test
    void deleteOfMissingOrderIsNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void createOrderConsumesMaterialsAndRemembersThem() {
        Material paper = new Material();
        paper.setId(7L);
        paper.setName("Картон");
        when(materialService.get(7L)).thenReturn(paper);
        when(clicheRepository.findById(3L)).thenReturn(Optional.of(cliche(customer, "75.00")));

        Order order = service.createOrder(customer, "Визитки", null, null, List.of(3L),
                Map.of(7L, new BigDecimal("50"), 8L, BigDecimal.ZERO));

        verify(materialService).reserve(eq(7L), eq(new BigDecimal("50")));
        verify(materialService, never()).reserve(eq(8L), any());
        ArgumentCaptor<OrderMaterial> captor = ArgumentCaptor.forClass(OrderMaterial.class);
        verify(orderMaterialRepository).save(captor.capture());
        assertThat(captor.getValue().getMaterial()).isSameAs(paper);
        assertThat(captor.getValue().getQuantity()).isEqualByComparingTo("50");
        assertThat(captor.getValue().getOrder()).isSameAs(order);
        assertThat(captor.getValue().isWrittenOff()).isFalse();
    }

    @Test
    void clicheMaterialIsReservedByTotalAreaWithMargins() {
        Material magnesium = new Material();
        magnesium.setId(9L);
        magnesium.setName("Магний");
        Cliche cl = cliche(customer, "75.00");   // 4 x 5, 1 шт.
        cl.setMarginCm(new BigDecimal("1"));       // общий размер 6 x 7 = 42 см²
        cl.setQuantity(2);                         // 84 см²
        cl.setMaterial(magnesium);
        when(clicheRepository.findById(3L)).thenReturn(Optional.of(cl));
        when(materialService.get(9L)).thenReturn(magnesium);

        Order order = service.createOrder(customer, "Клише", null, null, List.of(3L), null);

        verify(materialService).reserve(eq(9L), argThat(q -> q.compareTo(new BigDecimal("84")) == 0));
        assertThat(order.getDetails()).contains("= 20 см²").contains("общий размер 6 × 7 см = 42 см²").contains("поля 1 см");
    }

    @Test
    void shippingWritesOffReservedMaterialsAndReturnToWorkReservesAgain() {
        Order order = new Order();
        order.setId(4L);
        Material paper = new Material();
        paper.setId(7L);
        OrderMaterial used = new OrderMaterial();
        used.setOrder(order);
        used.setMaterial(paper);
        used.setQuantity(new BigDecimal("50"));
        when(orderRepository.findById(4L)).thenReturn(Optional.of(order));
        when(orderMaterialRepository.findByOrderId(4L)).thenReturn(List.of(used));

        service.setStatus(4L, OrderStatus.SHIPPED);

        verify(materialService).ship(eq(7L), eq(new BigDecimal("50")), any());
        assertThat(used.isWrittenOff()).isTrue();

        service.setStatus(4L, OrderStatus.READY);

        verify(materialService).unship(eq(7L), eq(new BigDecimal("50")), any());
        assertThat(used.isWrittenOff()).isFalse();
    }

    @Test
    void statusChangeBetweenNonShippedStatesDoesNotTouchStock() {
        Order order = new Order();
        order.setId(4L);
        when(orderRepository.findById(4L)).thenReturn(Optional.of(order));

        service.setStatus(4L, OrderStatus.IN_PROGRESS);

        verify(materialService, never()).ship(any(), any(), any());
        verify(materialService, never()).unship(any(), any(), any());
    }

    @Test
    void searchFindsOrdersByCustomerProductAndMaterial() {
        Order a = new Order();
        a.setId(1L);
        a.setCustomer(customer);
        a.setProduct("Визитки");
        Order b = new Order();
        b.setId(2L);
        b.setCustomer(other);
        b.setProduct("Открытки");
        Material paper = new Material();
        paper.setName("Мелованная 300");
        OrderMaterial used = new OrderMaterial();
        used.setOrder(b);
        used.setMaterial(paper);
        when(orderRepository.findAllByOrderByIdDesc()).thenReturn(List.of(b, a));
        when(orderMaterialRepository.findByOrderIdIn(any())).thenReturn(List.of(used));

        assertThat(service.search("firma a")).containsExactly(a);
        assertThat(service.search("визит")).containsExactly(a);
        assertThat(service.search("мелован")).containsExactly(b);
        assertThat(service.search("2")).containsExactly(b);
        assertThat(service.search("  ")).containsExactly(b, a);
    }

    @Test
    void summariesCountOrdersPerCustomerAndKeepCustomersWithoutOrders() {
        Customer empty = Customer.builder().id(30L).name("Firma C").build();
        Order shipped = new Order();
        shipped.setCustomer(customer);
        shipped.setStatus(OrderStatus.SHIPPED);
        shipped.setTotalCost(new BigDecimal("100"));
        Order open = new Order();
        open.setCustomer(customer);
        open.setTotalCost(new BigDecimal("50"));
        when(orderRepository.findAllByOrderByIdDesc()).thenReturn(List.of(open, shipped));

        List<OrderService.CustomerSummary> result = service.summaries(List.of(empty, customer));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getCustomer()).isSameAs(customer);
        assertThat(result.get(0).getOrderCount()).isEqualTo(2);
        assertThat(result.get(0).getActiveCount()).isEqualTo(1);
        assertThat(result.get(0).getTotal()).isEqualByComparingTo("150");
        assertThat(result.get(1).getOrderCount()).isZero();
    }

    @Test
    void insufficientStockPropagatesSoOrderIsRolledBack() {
        when(clicheRepository.findById(3L)).thenReturn(Optional.of(cliche(customer, "75.00")));
        doThrow(new BusinessRuleException("На складе недостаточно"))
                .when(materialService).reserve(eq(7L), any());

        assertThatThrownBy(() -> service.createOrder(customer, "x", null, null, List.of(3L),
                Map.of(7L, new BigDecimal("999"))))
                .isInstanceOf(BusinessRuleException.class);
        verify(orderMaterialRepository, never()).save(any());
    }

    @Test
    void deleteOfShippedOrderReturnsMaterialsToStock() {
        Order order = new Order();
        order.setId(3L);
        Material paper = new Material();
        paper.setId(7L);
        OrderMaterial used = new OrderMaterial();
        used.setOrder(order);
        used.setMaterial(paper);
        used.setQuantity(new BigDecimal("50"));
        used.setWrittenOff(true);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));
        when(orderMaterialRepository.findByOrderId(3L)).thenReturn(List.of(used));

        service.delete(3L);

        verify(materialService).receive(eq(7L), eq(new BigDecimal("50")), any());
        verify(orderMaterialRepository).delete(used);
        verify(orderRepository).delete(order);
    }

    @Test
    void deleteOfUnshippedOrderOnlyReleasesReserve() {
        Order order = new Order();
        order.setId(3L);
        Material paper = new Material();
        paper.setId(7L);
        OrderMaterial used = new OrderMaterial();
        used.setOrder(order);
        used.setMaterial(paper);
        used.setQuantity(new BigDecimal("50"));
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));
        when(orderMaterialRepository.findByOrderId(3L)).thenReturn(List.of(used));

        service.delete(3L);

        verify(materialService).release(7L, new BigDecimal("50"));
        verify(materialService, never()).receive(any(), any(), any());
        verify(orderRepository).delete(order);
    }
}
