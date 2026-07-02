package com.autohub.entity;

import com.autohub.enums.CustomerLeadStatus;
import com.autohub.enums.Role;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "Customer-Leads")
@Data
public class CustomerLead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String uniqueLeadId;

    private String customerName;

    private String customerMobile;

    private String customerCity;

    @CreationTimestamp
    private LocalDateTime enquiryDate;

    @Enumerated(EnumType.STRING)
    private CustomerLeadStatus leadStatus;

    @ManyToOne
    private Vehicle vehicle;

    @ManyToOne
    private Dealer dealer;
}
