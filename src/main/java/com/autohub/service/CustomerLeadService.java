package com.autohub.service;

import com.autohub.dto.CustomerLeadRequestDTO;
import com.autohub.dto.CustomerLeadResponseDTO;
import com.autohub.dto.CustomerLeadStatusRequestDTO;
import com.autohub.dto.MonthlyLeadAnalyticsDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface CustomerLeadService {


    CustomerLeadResponseDTO createLead(Long vehicleId, CustomerLeadRequestDTO leadRequestDTO);

    List<CustomerLeadResponseDTO> getDealerLeads(Long dealerId);

    CustomerLeadResponseDTO updateLeadStatus(Long leadId, CustomerLeadStatusRequestDTO requestDTO);

    List<MonthlyLeadAnalyticsDTO> getMonthlyLead(Long dealerId);

}
