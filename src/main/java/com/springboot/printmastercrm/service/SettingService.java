package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.exception.BusinessRuleException;
import com.springboot.printmastercrm.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class SettingService {

    /** Единственная строка настроек. Её создаёт миграция V4. */
    private static final Long SETTINGS_ID = 1L;

    @Autowired
    private SettingRepository settingRepository;

    public Setting getSettings() {
        return settingRepository.findById(SETTINGS_ID).orElseGet(Setting::new);
    }

    public void saveSettings(Setting settings) {
        if (isNegative(settings.getMontageWorkPrice()) || isNegative(settings.getOneOttiskPrice())
                || isNegative(settings.getOneQuadratMetterFoilPrice()) || isNegative(settings.getPricePrint())) {
            throw new BusinessRuleException("Цены не могут быть отрицательными");
        }
        // обновляем существующую строку по полям, а не делаем save() объекта с чужим id
        Setting existing = settingRepository.findById(SETTINGS_ID).orElseGet(Setting::new);
        existing.setMontageWorkPrice(settings.getMontageWorkPrice());
        existing.setOneOttiskPrice(settings.getOneOttiskPrice());
        existing.setOneQuadratMetterFoilPrice(settings.getOneQuadratMetterFoilPrice());
        existing.setPricePrint(settings.getPricePrint());
        settingRepository.save(existing);
    }

    private static boolean isNegative(BigDecimal value) {
        return value == null || value.signum() < 0;
    }

}
