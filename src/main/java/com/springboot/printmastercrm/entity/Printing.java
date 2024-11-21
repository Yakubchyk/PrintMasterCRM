package com.springboot.printmastercrm.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Entity
@Table(name = "printing")
@Getter
@Setter
@ToString
public class Printing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    private String papier; // плотность бумаги. 90g/m2 - 350g/m2
    private String colour; //печать с одной стороны или с двух. 4+0, 4+4
    private Integer quantity; //тираж
    private double totalCost;

}
