package com.autohub.service;

import com.autohub.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AdminService {

    //ALl DEALER
    List<DealerResponseDTO> allDealer();

    //COUNT OF DEALER
    DealerCountResponseDTO getTotalDealerCount();

    //ALL LEADS
    List<AllCustomerLeadResponseDTO> getAllCustomerLeads();

    //COUNT PENDING DEALER
    PendingDealerCountResponseDTO getPendingDealerCount();

    //ALL VEHICLE
    List<VehicleResponseDTO> getAllVehicle();

    //COUNT VEHICLE
    VehicleCountResponseDTO getTotalVehicleCount();

    //COUNT LEADS
    CustomerLeadCountResponseDTO getTotalCustomerLeadCount();

    //Count Revenue
    RevenueCountResponseDTO getTotalRevenue();

    List<AdminMonthlyDealerAnalyticsDTO> getMonthlyDealerAnalytics();

    List<AdminMonthlyLeadAnalyticsDTO> getMonthlyLead();

    List<AdminMonthlyRevenueDTO> getMonthlyRevenueAnalytics();



}
