package com.autohub.serviceImpl;

import com.autohub.dto.VehicleQrResponseDTO;
import com.autohub.entity.Dealer;
import com.autohub.entity.Vehicle;
import com.autohub.exception.ResourceNotFoundException;
import com.autohub.repository.VehicleRepository;
import com.autohub.service.VehicleQrService;
import com.autohub.util.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * Generates vehicle QR codes.
 * The QR code encodes only the direct vehicle page link (URL) so that scanning it
 * directly opens the vehicle webpage in any camera or QR scanner.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleQrServiceImpl implements VehicleQrService {

    private final VehicleRepository vehicleRepository;

    @Value("${frontend.base-url:https://caryanam.com}")
    private String frontendBaseUrl;

    private static final int QR_WIDTH = 400;
    private static final int QR_HEIGHT = 400;

    @Override
    public VehicleQrResponseDTO getVehicleQrCode(Long vehicleId) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found with id: " + vehicleId));

        Dealer dealer = vehicle.getDealer();
        if (dealer == null) {
            throw new ResourceNotFoundException(
                    "Dealer information not found for vehicle id: " + vehicleId);
        }

        String vehiclePageUrl = resolveVehiclePageUrl(vehicle.getId());

        // QR code encodes only the direct vehicle link
        byte[] qrPngBytes;
        try {
            qrPngBytes = QrCodeUtil.generateQrCodePng(vehiclePageUrl, QR_WIDTH, QR_HEIGHT);
        } catch (Exception e) {
            log.error("Failed to generate QR code for vehicle {}: {}", vehicle.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate QR code for vehicle: " + vehicle.getId(), e);
        }

        String qrBase64 = Base64.getEncoder().encodeToString(qrPngBytes);

        return VehicleQrResponseDTO.builder()
                .vehicleId(vehicle.getId())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .variant(vehicle.getVariant())
                .askingPrice(vehicle.getAskingPrice())
                .fuelType(vehicle.getFuelType())
                .city(vehicle.getCity())
                .registrationYear(vehicle.getRegistrationYear())
                .kilometerDriven(vehicle.getKilometerDriven())
                .dealerBusinessName(dealer.getBusinessName())
                .dealerName(dealer.getOwnerName())
                .dealerContactNumber(dealer.getDealerMobile())
                .dealerWhatsappNumber(dealer.getWhatsapp())
                .vehiclePageUrl(vehiclePageUrl)
                .qrCodeBase64(qrBase64)
                .build();
    }

    @Override
    public byte[] downloadVehicleQrCode(Long vehicleId) {

        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Vehicle not found with id: " + vehicleId));

        Dealer dealer = vehicle.getDealer();
        if (dealer == null) {
            throw new ResourceNotFoundException(
                    "Dealer information not found for vehicle id: " + vehicleId);
        }

        String vehiclePageUrl = resolveVehiclePageUrl(vehicle.getId());

        try {
            return QrCodeUtil.generateQrCodePng(vehiclePageUrl, QR_WIDTH, QR_HEIGHT);
        } catch (Exception e) {
            log.error("Failed to generate QR code PNG for vehicle {}: {}", vehicleId, e.getMessage(), e);
            throw new RuntimeException("Failed to generate QR code for vehicle: " + vehicleId, e);
        }
    }

    /**
     * Resolves the full vehicle public page URL ensuring proper base URL formatting.
     * The customer-facing vehicle details route is /car/{vehicleId}.
     */
    private String resolveVehiclePageUrl(Long vehicleId) {
        String baseUrl = (frontendBaseUrl != null && !frontendBaseUrl.isBlank())
                ? frontendBaseUrl.trim()
                : "https://caryanam.com";

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl + "/car/" + vehicleId;
    }
}
