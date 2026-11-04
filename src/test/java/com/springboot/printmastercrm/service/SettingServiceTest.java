package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.repository.SettingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SettingServiceTest {

    @Mock
    private SettingRepository settingRepository;
    @InjectMocks
    private SettingService service;

    private static Setting prices(String montage, String ottisk, String foil, String print) {
        Setting s = new Setting();
        s.setMontageWorkPrice(new BigDecimal(montage));
        s.setOneOttiskPrice(new BigDecimal(ottisk));
        s.setOneQuadratMetterFoilPrice(new BigDecimal(foil));
        s.setPricePrint(new BigDecimal(print));
        return s;
    }

    @Test
    void negativePriceIsRejected() {
        assertThatThrownBy(() -> service.saveSettings(prices("100", "-1", "50", "0.1")))
                .isInstanceOf(BusinessRuleException.class);
        verify(settingRepository, never()).save(any());
    }

    @Test
    void missingPriceIsRejected() {
        Setting s = prices("100", "2", "50", "0.1");
        s.setPricePrint(null);

        assertThatThrownBy(() -> service.saveSettings(s)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updatesTheSingleSettingsRow() {
        Setting existing = prices("1", "1", "1", "1");
        existing.setId(1L);
        when(settingRepository.findById(1L)).thenReturn(Optional.of(existing));
        Setting incoming = prices("100", "2", "50", "0.10");
        incoming.setId(999L); // чужой id из формы игнорируется

        service.saveSettings(incoming);

        verify(settingRepository).save(existing);
        assertThat(existing.getId()).isEqualTo(1L);
        assertThat(existing.getMontageWorkPrice()).isEqualByComparingTo("100");
        assertThat(existing.getPricePrint()).isEqualByComparingTo("0.10");
    }
}
