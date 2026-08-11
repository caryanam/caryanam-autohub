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
}
