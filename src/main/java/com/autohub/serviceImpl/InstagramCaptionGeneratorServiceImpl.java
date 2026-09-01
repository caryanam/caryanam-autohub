package com.autohub.serviceImpl;

import com.autohub.entity.Vehicle;
import com.autohub.entity.Dealer;
import com.autohub.service.InstagramCaptionGeneratorService;
import com.autohub.util.SocialPostVehicleUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Generates Instagram-optimised captions for vehicle posts.
 * Instagram supports up to 2,200 characters and 30 hashtags per post.
 * Captions include emojis and more hashtags compared to Facebook to
 * maximise engagement on the platform.
 */
@Service
public class InstagramCaptionGeneratorServiceImpl implements InstagramCaptionGeneratorService {

    @Value("${instagram.website-base-url:${spring.server.url}}")
    private String websiteBaseUrl;

    @Override
    public String generateCaption(Vehicle vehicle) {

        NumberFormat priceFormat = NumberFormat.getInstance(new Locale("en", "IN"));

        StringBuilder caption = new StringBuilder();

        // Title line with emoji
        caption.append("\uD83D\uDE97 ") // 🚗
                .append(vehicle.getRegistrationYear())
                .append(" ")
                .append(vehicle.getBrand())
                .append(" ")
                .append(vehicle.getModel())
                .append(" ")
                .append(vehicle.getVariant())
                .append("\n\n");

        // Vehicle details with emojis
        caption.append("\uD83D\uDCB0 Price: ₹").append(priceFormat.format(vehicle.getAskingPrice())).append("\n");
        caption.append("\uD83D\uDCC5 Year: ").append(vehicle.getRegistrationYear()).append("\n");
        caption.append("\uD83D\uDEE3\uFE0F KM Driven: ").append(priceFormat.format(vehicle.getKilometerDriven()))
                .append(" km\n");
        caption.append("⛽ Fuel: ").append(vehicle.getFuelType()).append("\n");
        caption.append("\uD83D\uDD11 Ownership: ").append(ordinal(vehicle.getOwnershipDetails())).append(" Owner\n");

        if (vehicle.getDealer() != null) {
            caption.append("\uD83C\uDFEA Dealer: ").append(vehicle.getDealer().getBusinessName()).append("\n");
        }

        caption.append("\uD83D\uDCCD Location: ").append(vehicle.getCity()).append("\n");

        // Call to action
        caption.append("\n\uD83D\uDC49 View details: ").append(buildVehicleUrl(vehicle)).append("\n\n");
        
        caption.append("📞For more information about this vehicle, please check the comment below for the dealer’s contact details. 👇\n\n");

        caption.append("Thank you! 😊\n\n");

        // Instagram-optimised hashtags (more than Facebook)
        caption.append(buildHashtags(vehicle));

        return caption.toString();
    }

    private String buildVehicleUrl(Vehicle vehicle) {
        return SocialPostVehicleUtil.buildVehicleListingUrl(websiteBaseUrl, vehicle.getId(), "instagram");
    }

    private String buildHashtags(Vehicle vehicle) {
        String brandTag = "#" + vehicle.getBrand().replaceAll("\\s+", "");
        String modelTag = "#" + vehicle.getModel().replaceAll("\\s+", "");
        String cityTag = "#" + vehicle.getCity().replaceAll("\\s+", "");
        return String.join(" ",
                "#Caryanam", "#UsedCars", "#UsedCarsIndia",
                "#SecondHandCar", "#PreOwnedCars",
                brandTag, modelTag, cityTag,
                "#CarDeals", "#CarsOfInstagram",
                "#BuyCar", "#CarForSale");
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

    @Override
    public String generateComment(Vehicle vehicle) {
        StringBuilder comment = new StringBuilder();
        NumberFormat priceFormat = NumberFormat.getInstance(new Locale("en", "IN"));

        comment.append("Vehicle Details:\n\n");

        if (vehicle.getAskingPrice() != null) {
            comment.append("💰 Price: ₹").append(priceFormat.format(vehicle.getAskingPrice())).append("\n");
        }
        if (vehicle.getRegistrationYear() != null) {
            comment.append("📅 Year: ").append(vehicle.getRegistrationYear()).append("\n");
        }
        if (vehicle.getKilometerDriven() != null) {
            comment.append("🛣️ KM Driven: ").append(priceFormat.format(vehicle.getKilometerDriven())).append(" km\n");
        }
        if (vehicle.getFuelType() != null && !vehicle.getFuelType().isBlank()) {
            comment.append("⛽ Fuel: ").append(vehicle.getFuelType()).append("\n");
        }
        comment.append("🔑 Ownership: ").append(ordinal(vehicle.getOwnershipDetails())).append(" Owner\n");
        if (vehicle.getCity() != null && !vehicle.getCity().isBlank()) {
            comment.append("📍 Location: ").append(vehicle.getCity()).append("\n");
        }

        if (vehicle.getDealer() != null && vehicle.getDealer().getBusinessName() != null) {
            comment.append("🏢 Dealer Name: ").append(vehicle.getDealer().getBusinessName()).append("\n");
        } else if (vehicle.getDealerContactName() != null && !vehicle.getDealerContactName().isBlank()) {
            comment.append("🏢 Dealer Name: ").append(vehicle.getDealerContactName()).append("\n");
        }

        comment.append("\nView more details here: ").append(buildVehicleUrl(vehicle));

        return comment.toString();
    }
}
