package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.PostPress;
import com.springboot.printmastercrm.entity.Setting;
import com.springboot.printmastercrm.repository.PostPressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

    public BigDecimal calculateTotal(PostPress postPress, Setting settings) {


        double quadratMetter;

        if ((postPress.getWidthSM() * postPress.getLengthSM()) <= MIN_AREA) {
            quadratMetter = MIN_AREA / COEFFICIENT_METER;
        } else {
            quadratMetter = (postPress.getWidthSM() * postPress.getLengthSM()) / COEFFICIENT_METER;
        }

        double totalFoilExpense = (((postPress.getQuantity() * quadratMetter) * COEFFICIENT_WORK) * settings.getOneQuadratMetterFoilPrice()) * COEFFICIENT_NDS;

        double totalWorkPrice = (settings.getMontageWorkPrice() + settings.getOneOttiskPrice() * postPress.getQuantity()) * COEFFICIENT_WORK;

        postPress.setTotalPrice(BigDecimal.valueOf(totalWorkPrice + totalFoilExpense));

        return postPress.getTotalPrice();


//        double rawArea = postPress.getWidthSM() * postPress.getLengthSM();
//        BigDecimal area = BigDecimal.valueOf(Math.max(rawArea, 50))
//                .divide(BigDecimal.valueOf(10000), RoundingMode.HALF_UP);
//
//        BigDecimal ndsCoefficient = BigDecimal.valueOf(1.25);
//        BigDecimal workCoefficient = BigDecimal.valueOf(1.05);
//
//        BigDecimal total = settings.getMontageWorkPrice().multiply(workCoefficient)
//                .add(settings.getOneOttiskPrice().multiply(new BigDecimal(postPress.getQuantity())).multiply(workCoefficient))
//                .add(settings.getFoilPrice().multiply(area).multiply(ndsCoefficient));
//
//        return total.setScale(2, RoundingMode.HALF_UP);
    }
}
