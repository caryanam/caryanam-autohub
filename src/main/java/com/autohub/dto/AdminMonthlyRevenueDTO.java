package com.autohub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class  AdminMonthlyRevenueDTO{

    private String month;
    private Double revenue;
}
