package com.autohub.serviceImpl;

import com.autohub.dto.*;
import com.autohub.entity.CustomerLead;
import com.autohub.entity.Dealer;
import com.autohub.entity.Vehicle;
import com.autohub.enums.CustomerLeadStatus;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.CustomerLeadRepository;
import com.autohub.repository.DealerRepository;
import com.autohub.repository.VehicleRepository;
import com.autohub.service.CustomerLeadService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
//@RequiredArgsConstructor
@Slf4j
public class CustomerLeadServiceImpl implements CustomerLeadService {

    private final CustomerLeadRepository leadRepository;
    private final VehicleRepository vehicleRepository;
    private final DealerRepository dealerRepository;
    private final ApplicationEventPublisher eventPublisher; // constructor-injected, not @Autowired

    public CustomerLeadServiceImpl(CustomerLeadRepository leadRepository,
                                   VehicleRepository vehicleRepository,
                                   DealerRepository dealerRepository,
                                   ApplicationEventPublisher eventPublisher) {
        this.leadRepository = leadRepository;
        this.vehicleRepository = vehicleRepository;
        this.dealerRepository = dealerRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public CustomerLeadResponseDTO createLead(Long vehicleId, CustomerLeadRequestDTO leadRequestDTO) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found"));

        Long dealerId = vehicle.getDealer().getId();

        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found"));

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uniquePart = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        String uniqueLeadId = "CAPL" + timestamp + vehicle.getId() + dealerId + uniquePart;

        CustomerLead lead = new CustomerLead();
        lead.setUniqueLeadId(uniqueLeadId);
        lead.setCustomerName(leadRequestDTO.getCustomerName());
        lead.setCustomerMobile(leadRequestDTO.getCustomerMobile());
        lead.setCustomerCity(leadRequestDTO.getCustomerCity());
        lead.setLeadStatus(CustomerLeadStatus.NEW);
        lead.setEnquiryDate(LocalDateTime.now());
        lead.setVehicle(vehicle);
        lead.setDealer(dealer);

        CustomerLead saved = leadRepository.save(lead);

        String vehicleDisplayName = vehicle.getBrand() + " " + vehicle.getModel();

        // Publish event with resolved primitive data. Listener will react only
        // AFTER this transaction commits - see LeadCreatedEventListener.
        eventPublisher.publishEvent(new LeadCreatedEvent(
                saved.getId(),
                vehicle.getId(),
                dealerId,
                saved.getCustomerName(),
                saved.getCustomerMobile(),
                vehicleDisplayName,
                dealer.getWhatsapp()
        ));

        log.info("Lead [{}] created successfully for vehicle [{}] and dealer [{}]",
                saved.getUniqueLeadId(), vehicleId, dealerId);

        return CustomerLeadResponseDTO.builder()
                .id(saved.getId())
                .uniqueLeadId(saved.getUniqueLeadId())
                .customerName(saved.getCustomerName())
                .customerMobile(saved.getCustomerMobile())
                .leadStatus(saved.getLeadStatus())
                .customerCity(saved.getCustomerCity())
                .vehicleName(vehicleDisplayName)
                .enquiryDate(saved.getEnquiryDate())
                .dealer(saved.getDealer().getId())
                .build();
    }

//
//    @Override
//    public CustomerLeadResponseDTO createLead(Long vehicleId, CustomerLeadRequestDTO leadRequestDTO) {
//
//        Vehicle vehicle = vehicleRepository.findById(vehicleId).orElseThrow(() ->
//                new ResourceNotFoundException("Vehicle not found with id: " + vehicleId));
//
//        Long dealerId = vehicle.getDealer().getId();
//
//        Dealer dealer = dealerRepository.findById(dealerId)
//                         .orElseThrow(() ->  new ResourceNotFoundException("Dealer not found"));
//
//        //Unique Lead ID
//
//        String timestamp = LocalDateTime.now()
//                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//
//        String uniquePart = UUID.randomUUID()
//                .toString()
//                .substring(0, 6)
//                .toUpperCase();
//
//        String uniqueLeadId = "CAPL" +timestamp +vehicle.getId()+dealerId+uniquePart;
//
//
//
//        CustomerLead lead = new CustomerLead();
//        lead.setUniqueLeadId(uniqueLeadId);
//        lead.setCustomerName(leadRequestDTO.getCustomerName());
//        lead.setCustomerMobile(leadRequestDTO.getCustomerMobile());
//        lead.setCustomerCity(leadRequestDTO.getCustomerCity());
//        lead.setLeadStatus(CustomerLeadStatus.NEW);
//        lead.setEnquiryDate(LocalDateTime.now());
//        lead.setVehicle(vehicle);
//        lead.setDealer(dealer);
//
//        CustomerLead saved = leadRepository.save(lead);
//
//        //Send Lead information to dealer via whatsapp
//
//        String vehicleName =
//                vehicle.getBrand()
//                        + " "
//                        + vehicle.getModel()
//                        + " "
//                        + vehicle.getVariant();
//
//        leadNotificationService.sendLeadToDealer(
//                dealer.getWhatsapp(), // dealer whatsapp no
//                saved.getCustomerName(),
//                saved.getCustomerMobile(),
//                vehicleName
//        );
//
//        return CustomerLeadResponseDTO.builder()
//                .id(saved.getId())
//                .uniqueLeadId(saved.getUniqueLeadId())
//                .customerName(saved.getCustomerName())
//                .customerMobile(saved.getCustomerMobile())
//                .leadStatus(saved.getLeadStatus())
//                .customerCity(saved.getCustomerCity())
//                .vehicleName(saved.getVehicle().getBrand() + " "+ saved.getVehicle().getModel())
//                .enquiryDate(saved.getEnquiryDate())
//                .dealer(saved.getDealer().getId())
//                .build();
//
//    }

    @Override
    public List<CustomerLeadResponseDTO> getDealerLeads(Long dealerId) {

        List<CustomerLead> leads = leadRepository.findByDealerId(dealerId);

        if (leads.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No leads found for dealer id : " + dealerId);
        }
        return leads.stream()
                .map(lead -> CustomerLeadResponseDTO.builder()
                        .id(lead.getId())
                        .uniqueLeadId(lead.getUniqueLeadId())
                        .customerName(lead.getCustomerName())
                        .customerMobile(lead.getCustomerMobile())
                        .customerCity(lead.getCustomerCity())
                        .vehicleName(lead.getVehicle().getBrand() + " "+ lead.getVehicle().getModel())
                        .dealer(lead.getDealer().getId())
                        .leadStatus(lead.getLeadStatus())
                        .enquiryDate(lead.getEnquiryDate())
                        .build())
                .toList();

    }

    @Override
    public CustomerLeadResponseDTO updateLeadStatus(Long leadId, CustomerLeadStatusRequestDTO requestDTO) {

        CustomerLead lead = leadRepository.findById(leadId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Dealer not found"));

        if (requestDTO.getStatus() == null ||
                requestDTO.getStatus().trim().isEmpty()) {

            throw new RuntimeException("Status is required");
        }

        CustomerLeadStatus newStatus;

        try {
            newStatus = CustomerLeadStatus.valueOf(
                    requestDTO.getStatus().trim().toUpperCase());
        } catch (Exception e) {
            throw new RuntimeException(
                    "Invalid status. Only PENDING or CONTACTED or CONVERTED are allowed");
        }

        CustomerLeadStatus currentStatus = lead.getLeadStatus();

        if (currentStatus == null) {
            currentStatus = CustomerLeadStatus.NEW;
        }

        if (currentStatus.equals(newStatus)) {
            throw new RuntimeException(
                    "Lead status already " + currentStatus);
        }

        lead.setLeadStatus(newStatus);

        CustomerLead saved = leadRepository.save(lead);

        return CustomerLeadResponseDTO.builder()
                .id(saved.getId())
                .uniqueLeadId(saved.getUniqueLeadId())
                .customerName(saved.getCustomerName())
                .customerMobile(saved.getCustomerMobile())
                .customerCity(saved.getCustomerCity())
                .vehicleName(saved.getVehicle().getBrand() + " "+ saved.getVehicle().getModel())
                .enquiryDate(saved.getEnquiryDate())
                .dealer(saved.getDealer().getId())
                .leadStatus(saved.getLeadStatus())
                .build();
    }

    @Override
    public List<MonthlyLeadAnalyticsDTO> getMonthlyLead(Long dealerId) {

        List<Object[]> result = leadRepository.getMonthlyLeadAnalytics(dealerId);

        String[] months = {
                "Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"
        };

        Map<Integer, MonthlyLeadAnalyticsDTO> map = new HashMap<>();

        for (Object[] row : result) {

            int month = ((Number) row[0]).intValue();
            Long leads = ((Number) row[1]).longValue();
            Long conversions = ((Number) row[2]).longValue();

            map.put(month,
                    new MonthlyLeadAnalyticsDTO(
                            months[month - 1],
                            leads,
                            conversions
                    ));
        }

        List<MonthlyLeadAnalyticsDTO> response = new ArrayList<>();

        for (int i = 1; i <= 12; i++) {

            response.add(
                    map.getOrDefault(
                            i,
                            new MonthlyLeadAnalyticsDTO(
                                    months[i - 1],
                                    0L,
                                    0L
                            )
                    )
            );
        }

        return response;
    }
}
