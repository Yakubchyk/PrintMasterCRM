package com.springboot.printmastercrm.repository;

import com.springboot.printmastercrm.entity.Setting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingRepository extends JpaRepository<Setting, Long> {
}
