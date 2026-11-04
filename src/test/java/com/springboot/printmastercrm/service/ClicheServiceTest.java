package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.AreaUnit;
import com.springboot.printmastercrm.entity.Cliche;
import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.repository.ClicheRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClicheServiceTest {

    @Mock
    private ClicheRepository clicheRepository;
    @Mock
    private SettingService settingService;
    @InjectMocks
    private ClicheService service;

    private static BigDecimal d(String value) {
        return new BigDecimal(value);
    }

    @Test
    void smallClicheCostsMinimumPrice() {
        // 4x5 = 20 см² * 0,5 = 10 < минимум 20; 3 шт: 20*3*1,25 = 75,00
        assertThat(service.calculateTotal(d("4"), d("5"), 3, TestSettings.standard())).isEqualByComparingTo("75.00");
    }

    @Test
    void largeClichePaysByArea() {
        // 10x10 = 100 см² * 0,5 = 50; 2 шт: 50*2*1,25 = 125,00
        assertThat(service.calculateTotal(d("10"), d("10"), 2, TestSettings.standard())).isEqualByComparingTo("125.00");
    }

    @Test
    void pricePerSquareMetreGivesSameResult() {
        Setting perM2 = TestSettings.standard();
        perM2.setClichePrice(d("5000"));
        perM2.setClicheAreaUnit(AreaUnit.M2);

        assertThat(service.calculateTotal(d("10"), d("10"), 2, perM2)).isEqualByComparingTo("125.00");
    }

    @Test
    void createStoresCalculatedTotal() {
        when(settingService.getSettings()).thenReturn(TestSettings.standard());
        when(clicheRepository.save(any(Cliche.class))).thenAnswer(i -> i.getArgument(0));

        Cliche saved = service.create(Customer.builder().id(1L).build(), d("10"), d("10"), 2);

        assertThat(saved.getTotalPrice()).isEqualByComparingTo("125.00");
        assertThat(saved.getQuantity()).isEqualTo(2);
    }

    @Test
    void createRejectsNonPositiveValues() {
        Customer customer = Customer.builder().id(1L).build();

        assertThatThrownBy(() -> service.create(customer, d("0"), d("10"), 1)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.create(customer, d("10"), d("10"), 0)).isInstanceOf(BusinessRuleException.class);
    }
}
