package com.autohub.service;

import com.autohub.entity.Vehicle;

/**
 * Builds the Facebook Page post caption for a vehicle. Kept as a pure,
 * side-effect-free service so it can be unit tested without touching the
 * database or the network.
 */
public interface CaptionGeneratorService {

    String generateCaption(Vehicle vehicle);
}
