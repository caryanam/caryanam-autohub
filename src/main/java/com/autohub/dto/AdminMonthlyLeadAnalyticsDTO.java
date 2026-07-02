package com.autohub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminMonthlyLeadAnalyticsDTO {

    private String month;
    private Long leads;
}
