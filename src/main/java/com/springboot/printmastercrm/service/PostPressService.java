package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.PostPress;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.repository.PostPressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class PostPressService {

    private static final double COEFFICIENT_METER = 10000.0;
    private static final double COEFFICIENT_NDS = 1.25;
    private static final double COEFFICIENT_WORK = 1.05;
    private static final int MIN_AREA = 50;

    @Autowired
    private PostPressRepository postPressRepository;

    @Autowired
    private SettingService settingService;

    public List<PostPress> findByCustomerId(Long customerId) {
        return postPressRepository.findByCustomerId(customerId);
    }

    public void save(PostPress postPress) {
        Setting settings = settingService.getSettings();
        BigDecimal totalCost = calculateTotal(postPress, settings);
        postPress.setTotalPrice(totalCost);
        postPressRepository.save(postPress);
    }


    public void deletePostPressById(Long id) {
        postPressRepository.deleteById(id);
    }

    public BigDecimal calculateTotal(PostPress postPress, Setting settings) {
        double quadratMetter;

        if ((postPress.getWidthSM() * postPress.getLengthSM()) <= MIN_AREA) {
            quadratMetter = MIN_AREA / COEFFICIENT_METER;
        } else {
            quadratMetter = (postPress.getWidthSM() * postPress.getLengthSM()) / COEFFICIENT_METER;
        }

        double totalFoilExpense = (((postPress.getQuantity() * quadratMetter) * COEFFICIENT_WORK) * settings.getOneQuadratMetterFoilPrice()) * COEFFICIENT_NDS;

        double totalWorkPrice = (settings.getMontageWorkPrice() + settings.getOneOttiskPrice() * postPress.getQuantity()) * COEFFICIENT_WORK;

        BigDecimal totalCost = BigDecimal.valueOf(totalWorkPrice + totalFoilExpense);

        totalCost = totalCost.setScale(2, BigDecimal.ROUND_HALF_UP);

        postPress.setTotalPrice(totalCost);

        return totalCost;
    }

//    public BigDecimal calculateTotal(PostPress postPress, Setting settings) {
//
//
//        double quadratMetter;
//
//        if ((postPress.getWidthSM() * postPress.getLengthSM()) <= MIN_AREA) {
//            quadratMetter = MIN_AREA / COEFFICIENT_METER;
//        } else {
//            quadratMetter = (postPress.getWidthSM() * postPress.getLengthSM()) / COEFFICIENT_METER;
//        }
//
//        double totalFoilExpense = (((postPress.getQuantity() * quadratMetter) * COEFFICIENT_WORK) * settings.getOneQuadratMetterFoilPrice()) * COEFFICIENT_NDS;
//
//        double totalWorkPrice = (settings.getMontageWorkPrice() + settings.getOneOttiskPrice() * postPress.getQuantity()) * COEFFICIENT_WORK;
//
//        postPress.setTotalPrice(BigDecimal.valueOf(totalWorkPrice + totalFoilExpense));
//
//        // Суммируем итоговые значения
//        BigDecimal totalCost = BigDecimal.valueOf(totalWorkPrice + totalFoilExpense);
//
//        // Округление до двух знаков после запятой
//        totalCost = totalCost.setScale(2, BigDecimal.ROUND_HALF_UP);
//
//
//        return postPress.getTotalPrice();
//    }

    public List<PostPress> findAll() {
        return postPressRepository.findAll();

    }

    public PostPress findById(Long postPressId) {
        return postPressRepository.findById(postPressId)
                .orElseThrow(() -> new IllegalArgumentException("PostPress not found with ID: " + postPressId));
    }


}
