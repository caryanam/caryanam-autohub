package com.autohub.entity;

import com.autohub.enums.InsuranceStatus;
import com.autohub.enums.VehicleStatus;
import com.autohub.enums.VehicleType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vehicle-info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {

    //ODL FIELDS REPLACE WITH DEALER MAPPING
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String variant;

    @Column(nullable = false)
    private Integer registrationYear;

    @Column(nullable = false)
    private Double askingPrice;

    @Column(nullable = false)
    private Long kilometerDriven;

    @Column(nullable = false,length = 100)
    private String fuelType;

    //@Column(nullable = false)
   // private String transmission;// automatic / normal

    //@Column(nullable = false)
    //private String rtoInformation;// mh12

    @Column(nullable = false)
    private boolean financeAvailability; //

    @Column(nullable = false)
    private int ownershipDetails;// 1st or 2nd

    //@Enumerated(EnumType.STRING)
    //private InsuranceStatus insuranceStatus;

    @Column(nullable = false)
    private String city;

    @Column(length = 5000,nullable = false)
    private String vehicleDescription;

    @Enumerated(EnumType.STRING)
    private VehicleType vehicleType;


    @Enumerated(EnumType.STRING)
    private VehicleStatus vehicleStatus;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private String dealerContactName;

    private String dealerContactNumber;

    private Long subLocalityId;

//    private  String dealerContactEmail;
//
//    private String dealerWhatsappNumber;
//
//    private String dealerBusinessName;

    @ManyToOne
    @JoinColumn(name = "dealerId")
    private Dealer dealer;

//    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL,orphanRemoval = true)
//    private List<VehicleMedia> mediaList;


    @OneToMany(
            mappedBy = "vehicle",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<VehicleMedia> mediaList = new ArrayList<>();
}
