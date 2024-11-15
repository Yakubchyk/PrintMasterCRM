package com.springboot.printmastercrm.service;

import com.springboot.printmastercrm.entity.PostPress;
import com.springboot.printmastercrm.repository.PostPressRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class PostPressService {

    private static final double COEFFICIENT_METER = 10000.0;
    private static final double COEFFICIENT_NDS = 1.25;
    private static final double COEFFICIENT_WORK = 1.05;
    private static final int MIN_AREA = 50;

    private final PostPressRepository postPressRepository;

    @Autowired
    public PostPressService(PostPressRepository postPressRepository) {
        this.postPressRepository = postPressRepository;
    }

    public BigDecimal calculateTotalCost(PostPress postPress) {
        double quadratMetter;

        if ((postPress.getWidthSM() * postPress.getLengthSM()) <= MIN_AREA) {
            quadratMetter = MIN_AREA / COEFFICIENT_METER;
        } else {
            quadratMetter = (postPress.getWidthSM() * postPress.getLengthSM()) / COEFFICIENT_METER;
        }

        double totalFoilExpense = ((postPress.getQuantity() * quadratMetter) * COEFFICIENT_WORK)
                * postPress.getOneQuadratMetterFoilPrice().doubleValue() * COEFFICIENT_NDS;

        double totalWorkPrice = (postPress.getMontageWorkPrice().doubleValue()
                + postPress.getOneOttiskPrice().doubleValue() * postPress.getQuantity()) * COEFFICIENT_WORK;

        return BigDecimal.valueOf(totalWorkPrice + totalFoilExpense).setScale(2, BigDecimal.ROUND_HALF_UP);
    }

    public PostPress savePostPress(PostPress postPress) {
        return postPressRepository.save(postPress);
    }
}