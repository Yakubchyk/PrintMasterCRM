package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Printing;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.repository.PrintingRepository;
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
class PrintingServiceTest {

    @Mock
    private PrintingRepository printingRepository;
    @Mock
    private SettingService settingService;
    @InjectMocks
    private PrintingService service;

    private static Setting settings() {
        Setting s = new Setting();
        s.setPricePrint(new BigDecimal("0.10"));
        return s;
    }

    private static Printing printing(String papier, String colour, Integer quantity) {
        Printing p = new Printing();
        p.setPapier(papier);
        p.setColour(colour);
        p.setQuantity(quantity);
        return p;
    }

    @Test
    void oneSidedLightPaper() {
        // 0,10 * 1 * 1000 * 1,0 * 1,25 = 125,00
        assertThat(service.calculateCost(printing("90g/m2", "4+0", 1000), settings())).isEqualByComparingTo("125.00");
    }

    @Test
    void twoSidedHeavierPaperCostsMore() {
        // 0,10 * 2 * 1000 * 1,2 * 1,25 = 300,00
        assertThat(service.calculateCost(printing("150g/m2", "4+4", 1000), settings())).isEqualByComparingTo("300.00");
    }

    @Test
    void densityMultipliers() {
        assertThat(service.calculateCost(printing("200g/m2", "4+0", 1000), settings())).isEqualByComparingTo("175.00");
        assertThat(service.calculateCost(printing("300g/m2", "4+0", 1000), settings())).isEqualByComparingTo("200.00");
    }

    @Test
    void unknownOrMissingPaperUsesBasePrice() {
        assertThat(service.calculateCost(printing("???", "4+0", 1000), settings())).isEqualByComparingTo("125.00");
        assertThat(service.calculateCost(printing(null, "4+0", 1000), settings())).isEqualByComparingTo("125.00");
    }

    @Test
    void quantityIsRequired() {
        assertThatThrownBy(() -> service.calculateCost(printing("90g/m2", "4+0", null), settings()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void saveStoresCalculatedCost() {
        when(settingService.getSettings()).thenReturn(settings());
        when(printingRepository.save(any(Printing.class))).thenAnswer(i -> i.getArgument(0));

        Printing saved = service.save(printing("90g/m2", "4+0", 1000));

        assertThat(saved.getTotalCost()).isEqualByComparingTo("125.00");
    }
}
