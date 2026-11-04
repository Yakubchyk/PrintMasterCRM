package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.AreaUnit;
import com.springboot.printmastercrm.entity.LengthUnit;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class SettingService {

    /** Единственная строка настроек. Её создаёт миграция V1. */
    private static final Long SETTINGS_ID = 1L;

    private final SettingRepository settingRepository;

    @Transactional
    public Setting getSettings() {
        return settingRepository.findById(SETTINGS_ID).orElseGet(() -> {
            Setting created = new Setting();
            created.setId(SETTINGS_ID);
            return settingRepository.save(created);
        });
    }

    @Transactional
    public Setting saveSettings(Setting form) {
        requireNotNegative(form.getMontageWorkPrice(), "Приладка");
        requireNotNegative(form.getOneOttiskPrice(), "Цена оттиска");
        requireNotNegative(form.getFoilPrice(), "Цена фольги");
        requireNotNegative(form.getPricePrint(), "Цена печати");
        requireNotNegative(form.getClichePrice(), "Цена клише");
        requireNotNegative(form.getClicheMinPrice(), "Минимальная цена клише");
        if (form.getMarkup() == null || form.getMarkup().signum() <= 0) {
            throw new BusinessRuleException("Коэффициент должен быть больше нуля");
        }
        LengthUnit lengthUnit = form.getLengthUnit() == null ? LengthUnit.CM : form.getLengthUnit();
        AreaUnit foilUnit = form.getFoilAreaUnit() == null ? AreaUnit.M2 : form.getFoilAreaUnit();
        AreaUnit clicheUnit = form.getClicheAreaUnit() == null ? AreaUnit.CM2 : form.getClicheAreaUnit();

        // обновляем существующую строку по полям, чужой id из формы не принимаем
        Setting existing = getSettings();
        existing.setLengthUnit(lengthUnit);
        existing.setMontageWorkPrice(form.getMontageWorkPrice());
        existing.setOneOttiskPrice(form.getOneOttiskPrice());
        existing.setFoilPrice(form.getFoilPrice());
        existing.setFoilAreaUnit(foilUnit);
        existing.setPricePrint(form.getPricePrint());
        existing.setClichePrice(form.getClichePrice());
        existing.setClicheAreaUnit(clicheUnit);
        existing.setClicheMinPrice(form.getClicheMinPrice());
        existing.setMarkup(form.getMarkup());
        return settingRepository.save(existing);
    }

    private static void requireNotNegative(BigDecimal value, String field) {
        if (value == null || value.signum() < 0) {
            throw new BusinessRuleException(field + ": укажите число не меньше нуля");
        }
    }
}
