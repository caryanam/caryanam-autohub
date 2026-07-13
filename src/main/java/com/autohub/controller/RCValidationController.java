package com.autohub.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/srv2/validation")
public class RCValidationController {

    @PostMapping("/rc")
    public ResponseEntity<String> validateRC(@RequestBody Map<String, Object> requestBody) {
        // IDSPay Production Base URL + Endpoint
        String idspayUrl = "https://javabackend.idspay.in/api/v1/prod/srv2/validation/rc";
        // Set up headers for the JSON request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Pass the request body received from the frontend directly to IDSPay
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        RestTemplate restTemplate = new RestTemplate();
        try {
            // Make the POST request to IDSPay
            ResponseEntity<String> response = restTemplate.postForEntity(idspayUrl, entity, String.class);
            // Return the response back to the frontend
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (HttpClientErrorException e) {
            // Forward IDSPay's error responses (e.g., 401, 422) directly to the frontend
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            // Handle unexpected server errors
            return ResponseEntity.status(500).body("{\"message\": \"Internal Server Error: " + e.getMessage() + "\", \"status\": 500}");
        }
    }
}