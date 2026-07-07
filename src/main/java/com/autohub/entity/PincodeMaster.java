package com.autohub.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "pincode_master")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PincodeMaster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String city;

    private String area;

    private String pincode;

    @Column(columnDefinition = "TEXT")
    private String nearBy;
}