package com.autohub.controller;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@RestController
@RequestMapping("/srv2") // Changed mapping to cover the base route
public class EChallanController {

    @PostMapping("/basic-e-challan")
    public ResponseEntity<String> checkEChallan(@RequestBody Map<String, Object> requestBody) {
        // IDSPay Production Base URL + Endpoint
        String idspayUrl = "https://javabackend.idspay.in/api/v1/prod/srv2/basic-e-challan";
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
            // Forward IDSPay's error responses directly to the frontend
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (Exception e) {
            // Handle unexpected server errors
            return ResponseEntity.status(500).body("{\"message\": \"Internal Server Error: " + e.getMessage() + "\", \"status\": 500}");
        }
    }
}