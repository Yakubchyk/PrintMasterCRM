package com.springboot.printmastercrm.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Название организации. */
    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String email;

    private String postalCode;

    private String city;

    private String street;

    private String houseNumber;

    /** Адрес одной строкой: «улица дом, индекс город»; пустые части пропускаются. */
    public String getAddressLine() {
        String line1 = join(" ", street, houseNumber);
        String line2 = join(" ", postalCode, city);
        return join(", ", line1, line2);
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(sep);
                }
                sb.append(part.trim());
            }
        }
        return sb.toString();
    }
}
