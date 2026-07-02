package com.autohub.serviceImpl;

import com.autohub.entity.CustomerLead;
import com.autohub.entity.Dealer;
import com.autohub.entity.Vehicle;
import com.autohub.enums.CustomerLeadStatus;
import com.autohub.repository.*;
import com.autohub.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final DealerRepository dealerRepository;
    private final CustomerLeadRepository leadRepository;
    private final VehicleRepository vehicleRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public ResponseEntity<Resource> dealerActivityReport() {

        List<Dealer> dealers = dealerRepository.findAll();

        StringBuilder csv = new StringBuilder();

        csv.append(
                "DealerId,BusinessName,OwnerName,City,Vehicles,Leads,ConvertedLeads,Plan\n"
        );

        for (Dealer dealer : dealers) {

            long vehicleCount =
                    vehicleRepository.countByDealerId(dealer.getId());

            long leadCount =
                    leadRepository.countByDealerId(dealer.getId());

            long convertedLeads =
                    leadRepository.countByDealerIdAndLeadStatus(
                            dealer.getId(),
                            CustomerLeadStatus.CONVERTED);

            csv.append(dealer.getId()).append(",")
                    .append(dealer.getBusinessName()).append(",")
                    .append(dealer.getOwnerName()).append(",")
                    .append(dealer.getCity()).append(",")
                    .append(vehicleCount).append(",")
                    .append(leadCount).append(",")
                    .append(convertedLeads).append(",")
                    .append(dealer.getSubscriptionPlan())
                    .append("\n");
        }

        return buildCsv(csv.toString(), "dealer-activity.csv");
    }

    @Override
    public ResponseEntity<Resource> leadConversionReport() {

        List<CustomerLead> leads = leadRepository.findAll();

        StringBuilder csv = new StringBuilder();

        csv.append(
                "LeadId,CustomerName,Mobile,City,Status\n"
        );

        for (CustomerLead lead : leads) {

            csv.append(lead.getId()).append(",")
                    .append(lead.getCustomerName()).append(",")
                    .append(lead.getCustomerMobile()).append(",")
                    .append(lead.getCustomerCity()).append(",")
                    .append(lead.getLeadStatus())
                    .append("\n");
        }

        return buildCsv(csv.toString(), "lead-conversion.csv");
    }

    @Override
    public ResponseEntity<Resource> revenueByPlanReport() {

        List<Object[]> report =
                paymentRepository.getRevenueByPlanReport();

        StringBuilder csv = new StringBuilder();

        csv.append("Plan,TotalPurchases,Revenue\n");

        for (Object[] row : report) {

            csv.append(row[0]).append(",")
                    .append(row[1]).append(",")
                    .append(row[2])
                    .append("\n");
        }

        return buildCsv(csv.toString(), "revenue-by-plan.csv");
    }

    @Override
    public ResponseEntity<Resource> topCitiesReport() {

        List<Object[]> report =
                dealerRepository.getTopCitiesReport();

        StringBuilder csv = new StringBuilder();

        csv.append("City,TotalDealers\n");

        for (Object[] row : report) {

            csv.append(row[0]).append(",")
                    .append(row[1])
                    .append("\n");
        }

        return buildCsv(csv.toString(), "top-cities.csv");
    }

    @Override
    public ResponseEntity<Resource> vehicleInventoryReport() {

        List<Vehicle> vehicles =
                vehicleRepository.findAll();

        StringBuilder csv = new StringBuilder();

        csv.append(
                "VehicleId,Brand,Model,Variant,Price,City,Dealer,Status\n"
        );

        for (Vehicle vehicle : vehicles) {

            csv.append(vehicle.getId()).append(",")
                    .append(vehicle.getBrand()).append(",")
                    .append(vehicle.getModel()).append(",")
                    .append(vehicle.getVariant()).append(",")
                    .append(vehicle.getAskingPrice()).append(",")
                    .append(vehicle.getCity()).append(",")
                    .append(vehicle.getDealer().getBusinessName()).append(",")
                    .append(vehicle.getVehicleStatus())
                    .append("\n");
        }

        return buildCsv(csv.toString(), "vehicle-inventory.csv");
    }

    private ResponseEntity<Resource> buildCsv(
            String csvContent,
            String fileName) {

        ByteArrayResource resource =
                new ByteArrayResource(
                        csvContent.getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=" + fileName)
                .contentType(MediaType.parseMediaType("text/csv"))
                .contentLength(resource.contentLength())
                .body(resource);
    }
}
