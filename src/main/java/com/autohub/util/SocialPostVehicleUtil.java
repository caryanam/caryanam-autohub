package com.autohub.util;

import com.autohub.entity.Vehicle;
import com.autohub.entity.VehicleMedia;

import java.util.Comparator;

/**
 * Vehicle has no isPrimary flag on its media list, so "primary image" is
 * defined consistently across the Facebook module as: the lowest-id IMAGE
 * entry in the vehicle's media list (i.e. the first one uploaded).
 */
public final class SocialPostVehicleUtil {

    private SocialPostVehicleUtil() {
    }

    public static VehicleMedia findPrimaryImage(Vehicle vehicle) {
        if (vehicle.getMediaList() == null) {
            return null;
        }
        return vehicle.getMediaList().stream()
                .filter(m -> "IMAGE".equalsIgnoreCase(m.getMediaType()))
                .min(Comparator.comparing(VehicleMedia::getId))
                .orElse(null);
    }

    /**
     * Builds the public HTTPS URL Facebook will fetch the image from.
     * serverUrl is the same host used to serve /uploads/** static content
     * (spring.server.url), NOT the customer-facing website domain.
     */
    public static String buildImageUrl(String serverUrl, VehicleMedia media) {
        String base = serverUrl.endsWith("/") ? serverUrl.substring(0, serverUrl.length() - 1) : serverUrl;
        String path = media.getFilePath().replace("\\", "/");
        return base + (path.startsWith("/") ? path : "/" + path);
    }

    /** Builds the customer-facing "view this vehicle" link used in captions. */
    public static String buildVehicleListingUrl(String websiteBaseUrl, Long vehicleId) {
        String base = websiteBaseUrl.endsWith("/")
                ? websiteBaseUrl.substring(0, websiteBaseUrl.length() - 1)
                : websiteBaseUrl;
        return base + "/vehicle/" + vehicleId;
    }
}
