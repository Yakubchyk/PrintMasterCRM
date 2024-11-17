package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.repository.SettingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SettingService {

    @Autowired
    private SettingRepository settingRepository;

    // Получение текущих настроек (с одним экземпляром в базе)
    public Setting getSettings() {
        return settingRepository.findById(1L).orElse(new Setting());
    }

    // Сохранение обновленных настроек
    public void saveSettings(Setting settings) {
        settings.setId(1L); // Устанавливаем фиксированный ID, чтобы поддерживать единственную запись
        settingRepository.save(settings);
    }
}