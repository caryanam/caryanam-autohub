package com.autohub.serviceImpl;

import com.autohub.dto.*;
import com.autohub.entity.CustomerLead;
import com.autohub.entity.Dealer;
import com.autohub.entity.Vehicle;
import com.autohub.enums.DealerStatus;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.*;
import com.autohub.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final DealerRepository dealerRepository;

    private final CustomerLeadRepository customerLeadRepository;

    private final VehicleRepository vehicleRepository;

    private final PaymentRepository paymentRepository;

    private final EntityManager entityManager;

    @Value("${server.port}")
    private String port;

    @Value("${spring.server.url}")
    private String serverUrl;

    //All dealer
    @Override
    public List<DealerResponseDTO> allDealer() {

        List<Dealer> all = dealerRepository.findAll();

        if (all.isEmpty()) {
            throw new ResourceNotFoundException("Dealer has no vehicles");
        }

        return all.stream()
                .map(dealer -> DealerResponseDTO.builder()
                        .id(dealer.getId())
                        .businessName(dealer.getBusinessName())
                        .ownerName(dealer.getOwnerName())
                        .gstNumber(dealer.getGstNumber())
                        .yearsInBusiness(dealer.getYearsInBusiness())
                        .dealerMobile(dealer.getDealerMobile())
                        .executiveMobile(dealer.getExecutiveMobile())
                        .whatsapp(dealer.getWhatsapp())
                        .email(dealer.getEmail())
                        .address(dealer.getAddress())
                        .city(dealer.getCity())
                        .state(dealer.getState())
                        .pinCode(dealer.getPinCode())
                        .dealerLogo(buildMediaUrl(dealer.getDealerLogo()))
                        .showroomImage(buildMediaUrl(dealer.getShowroomImage()))
                        .dealerAccountStatus(dealer.getDealerAccountStatus())
                        .createdAt(dealer.getCreatedAt())
                        .build())
                .toList();
    }

 private String buildMediaUrl(String storedPath) {

     if (storedPath == null || storedPath.isBlank()) {
         return null;
     }

     String normalized = storedPath.replace("\\", "/").trim();

     if (!normalized.startsWith("/")) {
         normalized = "/" + normalized;
     }

     return serverUrl + normalized;
 }


    //Dealer Count
    @Override
    public DealerCountResponseDTO getTotalDealerCount() {

        return DealerCountResponseDTO.builder()
                .totalDealers(dealerRepository.count())
                .build();
    }


    //All leads
    @Override
    public List<AllCustomerLeadResponseDTO> getAllCustomerLeads() {

        List<CustomerLead> allLeads = customerLeadRepository.findAll();

        return allLeads.stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .map(lead -> AllCustomerLeadResponseDTO.builder()
                        .id(lead.getId())
                        .uniqueLeadId(lead.getUniqueLeadId())
                        .customerName(lead.getCustomerName())
                        .customerMobile(lead.getCustomerMobile())
                        .customerCity(lead.getCustomerCity())
                        .dealerName(lead.getDealer().getOwnerName())
                        .leadStatus(lead.getLeadStatus())
                        .enquiryDate(lead.getEnquiryDate())
                        .vehicleName(lead.getVehicle().getBrand()+" "+lead.getVehicle().getBrand()+" "+lead.getVehicle().getRegistrationYear())
                        .build())
                .toList();
    }

    //All pending dealer
    @Override
    public PendingDealerCountResponseDTO getPendingDealerCount() {

        long count = dealerRepository.countByDealerAccountStatus(DealerStatus.PENDING);

        return PendingDealerCountResponseDTO.builder()
                .totalPendingDealers(count)
                .build();
    }

    @Override
    public List<VehicleResponseDTO> getAllVehicle() {
        List<Vehicle> vehicles = vehicleRepository.findAll();

        if (vehicles.isEmpty()) {
            throw new ResourceNotFoundException("No Vehicles");
        }

        return vehicles.stream()
                .map(vehicle -> VehicleResponseDTO.builder()
                        .id(vehicle.getId())
                        .dealerId(vehicle.getDealer().getId())
                        .brand(vehicle.getBrand())
                        .model(vehicle.getModel())
                        .variant(vehicle.getVariant())
                        .registrationYear(vehicle.getRegistrationYear())
                        .askingPrice(BigDecimal.valueOf(vehicle.getAskingPrice()))
                        .kilometerDriven(vehicle.getKilometerDriven())
                        .fuelType(vehicle.getFuelType())
                      //  .transmission(vehicle.getTransmission())
                        .ownershipDetails(vehicle.getOwnershipDetails())
                      //  .insuranceStatus(String.valueOf(vehicle.getInsuranceStatus()))
                        .vehicleDescription(vehicle.getVehicleDescription())
                        .city(vehicle.getCity())
                        .dealerContactName(vehicle.getDealer().getOwnerName())
                        .dealerContactNumber(vehicle.getDealer().getDealerMobile())
                        .dealerWhatsappNumber(vehicle.getDealer().getWhatsapp())
                        .dealerBusinessName(vehicle.getDealer().getBusinessName())
                        .dealerContactEmail(vehicle.getDealer().getEmail())
                        .vehicleStatus(vehicle.getVehicleStatus())
                        .vehicleType(vehicle.getVehicleType())
                        .createdAt(vehicle.getCreatedAt())
                        .images(
                                vehicle.getMediaList() == null
                                        ? List.of()
                                        : vehicle.getMediaList().stream()
                                        .filter(media -> "IMAGE".equalsIgnoreCase(media.getMediaType()))
                                        //.map(VehicleMedia::getFilePath)
                                        .map(media -> serverUrl+
                                                media.getFilePath().replace("\\", "/"))
                                        .toList()
                        )

                        .videos(
                                vehicle.getMediaList() == null
                                        ? List.of()
                                        : vehicle.getMediaList().stream()
                                        .filter(media -> "VIDEO".equalsIgnoreCase(media.getMediaType()))
                                        //.map(VehicleMedia::getFilePath)
                                        .map(media ->serverUrl+
                                                media.getFilePath().replace("\\", "/"))
                                        .toList()
                        )

                        .build())
                .toList();
    }

    @Override
    public VehicleCountResponseDTO getTotalVehicleCount() {

        return VehicleCountResponseDTO.builder()
                .totalVehicles(vehicleRepository.count())
                .build();
    }

    @Override
    public RevenueCountResponseDTO getTotalRevenue() {

        return RevenueCountResponseDTO.builder()
                .totalRevenue(paymentRepository.getTotalRevenue())
                .build();
    }

    @Override
    public CustomerLeadCountResponseDTO getTotalCustomerLeadCount() {

        return CustomerLeadCountResponseDTO.builder()
                .totalCustomerLeads(customerLeadRepository.count())
                .build();
    }

    @Override
    public List<AdminMonthlyLeadAnalyticsDTO> getMonthlyLead() {

        List<Object[]> result = customerLeadRepository.getMonthlyLeadAnalytics();

        String[] months = {
                "Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"
        };

        Map<Integer, AdminMonthlyLeadAnalyticsDTO> map = new HashMap<>();

        for (Object[] row : result) {

            int month = ((Number) row[0]).intValue();
            Long leads = ((Number) row[1]).longValue();

            map.put(
                    month,
                    new AdminMonthlyLeadAnalyticsDTO(
                            months[month - 1],
                            leads
                    )
            );
        }

        List<AdminMonthlyLeadAnalyticsDTO> response = new ArrayList<>();

        for (int i = 1; i <= 12; i++) {

            response.add(
                    map.getOrDefault(
                            i,
                            new AdminMonthlyLeadAnalyticsDTO(
                                    months[i - 1],
                                    0L
                            )
                    )
            );
        }

        return response;
    }

    @Override
    public List<AdminMonthlyDealerAnalyticsDTO> getMonthlyDealerAnalytics() {

        List<Object[]> result = dealerRepository.getMonthlyDealerAnalytics();

        String[] months = {
                "Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"
        };

        Map<Integer, AdminMonthlyDealerAnalyticsDTO> map = new HashMap<>();

        for (Object[] row : result) {

            int month = ((Number) row[0]).intValue();
            Long dealers = ((Number) row[1]).longValue();

            map.put(
                    month,
                    new AdminMonthlyDealerAnalyticsDTO(
                            months[month - 1],
                            dealers
                    )
            );
        }

        List<AdminMonthlyDealerAnalyticsDTO> response = new ArrayList<>();

        for (int i = 1; i <= 12; i++) {

            response.add(
                    map.getOrDefault(
                            i,
                            new AdminMonthlyDealerAnalyticsDTO(
                                    months[i - 1],
                                    0L
                            )
                    )
            );
        }

        return response;
    }

    @Override
    public List<AdminMonthlyRevenueDTO> getMonthlyRevenueAnalytics() {

        List<Object[]> result =
                paymentRepository.getMonthlyRevenueAnalytics();

        String[] months = {
                "Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"
        };

        Map<Integer, AdminMonthlyRevenueDTO> map = new HashMap<>();

        for (Object[] row : result) {

            int month = ((Number) row[0]).intValue();

            Double revenue = row[1] == null
                    ? 0.0
                    : ((Number) row[1]).doubleValue();

            map.put(
                    month,
                    new AdminMonthlyRevenueDTO(
                            months[month - 1],
                            revenue
                    )
            );
        }

        List<AdminMonthlyRevenueDTO> response = new ArrayList<>();

        for (int i = 1; i <= 12; i++) {

            response.add(
                    map.getOrDefault(
                            i,
                            new AdminMonthlyRevenueDTO(
                                    months[i - 1],
                                    0.0
                            )
                    )
            );
        }

        return response;
    }

    @Override
    @Transactional
    public void deleteDealer(Long dealerId) {
        Dealer dealer = dealerRepository.findById(dealerId)
                .orElseThrow(() -> new ResourceNotFoundException("Dealer not found with id: " + dealerId));

        // Delete from WhatsappBirthdayMessageLog
        entityManager.createQuery("DELETE FROM WhatsappBirthdayMessageLog w WHERE w.dealerId = :dealerId")
                .setParameter("dealerId", dealerId).executeUpdate();

        // Delete from WhatsappMessageLog
        entityManager.createQuery("DELETE FROM WhatsappMessageLog w WHERE w.dealerId = :dealerId")
                .setParameter("dealerId", dealerId).executeUpdate();

        // Delete from WhatsappOfferMessageLog
        entityManager.createQuery("DELETE FROM WhatsappOfferMessageLog w WHERE w.dealerId = :dealerId")
                .setParameter("dealerId", dealerId).executeUpdate();

        // Delete from WhatsappVehicleShareLog
        entityManager.createQuery("DELETE FROM WhatsappVehicleShareLog w WHERE w.dealerId = :dealerId")
                .setParameter("dealerId", dealerId).executeUpdate();


        // Delete from CustomerLead
        entityManager.createQuery("DELETE FROM CustomerLead c WHERE c.dealer.id = :dealerId")
                .setParameter("dealerId", dealerId).executeUpdate();

        // Delete from Payment
        entityManager.createQuery("DELETE FROM Payment p WHERE p.dealer.id = :dealerId")
                .setParameter("dealerId", dealerId).executeUpdate();

        // Delete from SocialPostBatchItem (via SocialPostBatch)
        entityManager.createQuery("DELETE FROM SocialPostBatchItem i WHERE i.batch IN (SELECT b FROM SocialPostBatch b WHERE b.dealer.id = :dealerId)")
                .setParameter("dealerId", dealerId).executeUpdate();

        // Delete from SocialPostBatch
        entityManager.createQuery("DELETE FROM SocialPostBatch b WHERE b.dealer.id = :dealerId")
                .setParameter("dealerId", dealerId).executeUpdate();

        // Delete from VehicleSocialPostRequest
        entityManager.createQuery("DELETE FROM VehicleSocialPostRequest v WHERE v.dealer.id = :dealerId")
                .setParameter("dealerId", dealerId).executeUpdate();

        // Delete from VehicleView
        entityManager.createQuery("DELETE FROM VehicleView v WHERE v.dealer.id = :dealerId")
                .setParameter("dealerId", dealerId).executeUpdate();

        // Delete from Wishlist (via Vehicle)
        entityManager.createQuery("DELETE FROM Wishlist w WHERE w.vehicle IN (SELECT v FROM Vehicle v WHERE v.dealer.id = :dealerId)")
                .setParameter("dealerId", dealerId).executeUpdate();

        // Delete from VehicleMedia (via Vehicle)
        entityManager.createQuery("DELETE FROM VehicleMedia m WHERE m.vehicle IN (SELECT v FROM Vehicle v WHERE v.dealer.id = :dealerId)")
                .setParameter("dealerId", dealerId).executeUpdate();

        // Delete from Vehicle
        entityManager.createQuery("DELETE FROM Vehicle v WHERE v.dealer.id = :dealerId")
                .setParameter("dealerId", dealerId).executeUpdate();

        // Chat messages and chat rooms
        entityManager.createQuery("DELETE FROM ChatMessage c WHERE (c.senderId = :dealerId AND c.senderRole = 'DEALER') OR (c.receiverId = :dealerId AND c.receiverRole = 'DEALER')")
                .setParameter("dealerId", dealerId).executeUpdate();
        entityManager.createQuery("DELETE FROM ChatRoom c WHERE (c.user1Id = :dealerId AND c.user1Role = 'DEALER') OR (c.user2Id = :dealerId AND c.user2Role = 'DEALER')")
                .setParameter("dealerId", dealerId).executeUpdate();

        // Delete the dealer
        dealerRepository.delete(dealer);
    }
}
