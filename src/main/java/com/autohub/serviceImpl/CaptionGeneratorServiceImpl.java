package com.autohub.serviceImpl;

import com.autohub.entity.Vehicle;
import com.autohub.service.CaptionGeneratorService;
import com.autohub.util.SocialPostVehicleUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;

@Service
public class CaptionGeneratorServiceImpl implements CaptionGeneratorService {

    // Public website base URL used to build the "view vehicle" link in the
    // caption. Distinct from spring.server.url, which points at the API
    // host (c1.caryanam.com) rather than the customer-facing site.
    @Value("${facebook.website-base-url:${spring.server.url}}")
    private String websiteBaseUrl;

    @Override
    public String generateCaption(Vehicle vehicle) {

        NumberFormat priceFormat = NumberFormat.getInstance(new Locale("en", "IN"));

        StringBuilder caption = new StringBuilder();

        caption.append(vehicle.getRegistrationYear())
                .append(" ")
                .append(vehicle.getBrand())
                .append(" ")
                .append(vehicle.getModel())
                .append(" ")
                .append(vehicle.getVariant())
                .append("\n\n");

        caption.append("Price: Rs. ").append(priceFormat.format(vehicle.getAskingPrice())).append("\n");
        caption.append("Year: ").append(vehicle.getRegistrationYear()).append("\n");
        caption.append("KM Driven: ").append(priceFormat.format(vehicle.getKilometerDriven())).append(" km\n");
        caption.append("Fuel: ").append(vehicle.getFuelType()).append("\n");
        caption.append("Ownership: ").append(ordinal(vehicle.getOwnershipDetails())).append(" Owner\n");
        caption.append("Location: ").append(vehicle.getCity()).append("\n");

        if (vehicle.getDealer() != null) {
            caption.append("Dealer: ").append(vehicle.getDealer().getBusinessName()).append("\n");

            String contactNumber = vehicle.getDealer().getDealerMobile();
            if (contactNumber != null && !contactNumber.isBlank()) {
                caption.append("Contact: ").append(contactNumber).append("\n");
            }
        }

        caption.append("\n").append(buildVehicleUrl(vehicle)).append("\n\n");

        caption.append(buildHashtags(vehicle));

        return caption.toString();
    }

    private String buildVehicleUrl(Vehicle vehicle) {
        return SocialPostVehicleUtil.buildVehicleListingUrl(websiteBaseUrl, vehicle.getId());
    }

    private String buildHashtags(Vehicle vehicle) {
        String brandTag = "#" + vehicle.getBrand().replaceAll("\\s+", "");
        String modelTag = "#" + vehicle.getModel().replaceAll("\\s+", "");
        String cityTag = "#" + vehicle.getCity().replaceAll("\\s+", "");
        return String.join(" ", "#Caryanam", "#UsedCars", brandTag, modelTag, cityTag);
    }

    private String ordinal(int n) {
        if (n <= 0) {
            return n + "th";
        }
        return switch (n % 100) {
            case 11, 12, 13 -> n + "th";
            default -> switch (n % 10) {
                case 1 -> n + "st";
                case 2 -> n + "nd";
                case 3 -> n + "rd";
                default -> n + "th";
            };
        };
    }
}
