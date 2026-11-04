package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Customer;
import com.springboot.printmastercrm.entity.Printing;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.repository.PrintingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrintingServiceTest {

    @Mock
    private PrintingRepository printingRepository;
    @Mock
    private SettingService settingService;
    @InjectMocks
    private PrintingService service;

    @Test
    void oneSidedLightPaper() {
        // 0,10 * 1 * 1000 * 1,0 * 1,25 = 125,00
        assertThat(service.calculateCost("90g/m2", "4+0", 1000, TestSettings.standard())).isEqualByComparingTo("125.00");
    }

    @Test
    void twoSidedHeavierPaperCostsMore() {
        // 0,10 * 2 * 1000 * 1,2 * 1,25 = 300,00
        assertThat(service.calculateCost("150g/m2", "4+4", 1000, TestSettings.standard())).isEqualByComparingTo("300.00");
    }

    @Test
    void densityMultipliers() {
        assertThat(service.calculateCost("200g/m2", "4+0", 1000, TestSettings.standard())).isEqualByComparingTo("175.00");
        assertThat(service.calculateCost("300g/m2", "4+0", 1000, TestSettings.standard())).isEqualByComparingTo("200.00");
    }

    @Test
    void unknownOrMissingPaperUsesBasePrice() {
        assertThat(service.calculateCost("???", "4+0", 1000, TestSettings.standard())).isEqualByComparingTo("125.00");
        assertThat(service.calculateCost(null, "4+0", 1000, TestSettings.standard())).isEqualByComparingTo("125.00");
    }

    @Test
    void quantityIsRequired() {
        assertThatThrownBy(() -> service.calculateCost("90g/m2", "4+0", null, TestSettings.standard()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createStoresCalculatedCost() {
        when(settingService.getSettings()).thenReturn(TestSettings.standard());
        when(printingRepository.save(any(Printing.class))).thenAnswer(i -> i.getArgument(0));

        Printing saved = service.create(Customer.builder().id(1L).build(), "90g/m2", "4+0", 1000);

        assertThat(saved.getTotalCost()).isEqualByComparingTo("125.00");
    }

    @Test
    void createRejectsUnknownPaperColourAndBadQuantity() {
        Customer customer = Customer.builder().id(1L).build();

        assertThatThrownBy(() -> service.create(customer, "70g/m2", "4+0", 10)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.create(customer, "90g/m2", "1+0", 10)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.create(customer, "90g/m2", "4+0", 0)).isInstanceOf(BusinessRuleException.class);
    }
}
