package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.AreaUnit;
import com.springboot.printmastercrm.entity.LengthUnit;
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

    @Test
    void negativePriceIsRejected() {
        Setting form = TestSettings.standard();
        form.setOneOttiskPrice(new BigDecimal("-1"));

        assertThatThrownBy(() -> service.saveSettings(form)).isInstanceOf(BusinessRuleException.class);
        verify(settingRepository, never()).save(any());
    }

    @Test
    void missingPriceIsRejected() {
        Setting form = TestSettings.standard();
        form.setClichePrice(null);

        assertThatThrownBy(() -> service.saveSettings(form)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void markupMustBePositive() {
        Setting form = TestSettings.standard();
        form.setMarkup(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.saveSettings(form)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void updatesTheSingleSettingsRowAndIgnoresIncomingId() {
        Setting existing = new Setting();
        existing.setId(1L);
        when(settingRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(settingRepository.save(any(Setting.class))).thenAnswer(i -> i.getArgument(0));
        Setting form = TestSettings.standard();
        form.setId(999L);
        form.setLengthUnit(LengthUnit.MM);
        form.setFoilAreaUnit(AreaUnit.CM2);

        Setting saved = service.saveSettings(form);

        assertThat(saved).isSameAs(existing);
        assertThat(saved.getId()).isEqualTo(1L);
        assertThat(saved.getLengthUnit()).isEqualTo(LengthUnit.MM);
        assertThat(saved.getFoilAreaUnit()).isEqualTo(AreaUnit.CM2);
        assertThat(saved.getMontageWorkPrice()).isEqualByComparingTo("100");
        assertThat(saved.getMarkup()).isEqualByComparingTo("1.25");
    }

    @Test
    void missingRowIsCreatedWithDefaults() {
        when(settingRepository.findById(1L)).thenReturn(Optional.empty());
        when(settingRepository.save(any(Setting.class))).thenAnswer(i -> i.getArgument(0));

        Setting settings = service.getSettings();

        assertThat(settings.getId()).isEqualTo(1L);
        assertThat(settings.getMarkup()).isEqualByComparingTo("1.25");
    }
}
