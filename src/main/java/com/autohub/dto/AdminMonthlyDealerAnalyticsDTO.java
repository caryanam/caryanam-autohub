package com.autohub.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminMonthlyDealerAnalyticsDTO {

    private String month;
    private Long dealer;
}
