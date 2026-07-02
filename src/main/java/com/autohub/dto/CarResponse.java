package com.autohub.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CarResponse {

    private Long id;

    private String brand;

    private String carName;

    private Double price;

    private List<CarImageResponse> images;
}