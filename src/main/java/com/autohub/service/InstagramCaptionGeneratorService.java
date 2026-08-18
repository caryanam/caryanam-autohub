package com.autohub.service;

import com.autohub.entity.Vehicle;

/**
 * Builds the Instagram post caption for a vehicle. Kept as a pure,
 * side-effect-free service so it can be unit tested without touching the
 * database or the network. Instagram captions support up to 2,200
 * characters and 30 hashtags.
 */
public interface InstagramCaptionGeneratorService {

    String generateCaption(Vehicle vehicle);

    /**
     * Builds a comment to be posted on the vehicle's Instagram post.
     * Contains full vehicle details and the vehicle listing URL so
     * users can easily find it.
     */
    String generateComment(Vehicle vehicle);
}
