package com.autohub.controller;

import com.autohub.dto.PaymentRequestDTO;
import com.autohub.dto.ResponseDto;
import com.autohub.entity.Dealer;
import com.autohub.entity.Payment;
import com.autohub.enums.PaymentStatus;
import com.autohub.repository.DealerRepository;
import com.autohub.repository.PaymentRepository;
import com.autohub.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

// ================== DO PAYMENT FOR SUBSCRIPTION PLAN ======================
    @PostMapping("/subscription/purchase")
    @Operation(summary = "Payment for purchase subscription API ")
    public ResponseEntity<ResponseDto<?>> createPayment(
            @RequestBody PaymentRequestDTO dto) {

        return ResponseEntity.ok(paymentService.createPayment(dto));
    }

// ============ APPROVED DEALER PAYMENT STATUS BY ADMIN ===========

    @PutMapping("/success/{paymentId}")
    @Operation(summary = "Approve ( SUCCESS ) purchased subscription of dealer by Admin API ")
    public ResponseEntity<ResponseDto<?>> paymentSuccess(
            @PathVariable Long paymentId) {

        return ResponseEntity.ok(
                paymentService.paymentSuccess(paymentId));
    }

    @PutMapping("/failed/{paymentId}")
    @Operation(summary = "FAILED purchased subscription of dealer by Admin API")
    public ResponseEntity<ResponseDto<?>> paymentFailed(
            @PathVariable Long paymentId) {

        return ResponseEntity.ok(
                paymentService.paymentFailed(paymentId));
    }
    @GetMapping("/admin/history")
    @Operation(summary = "Get all dealer subscription payment history by Admin API ")
    public ResponseEntity<ResponseDto<?>> getAllPayments() {

        return ResponseEntity.ok(
                paymentService.getAllPayments());
    }


    @GetMapping("/dealer/{dealerId}")
    @Operation(summary = "Get Dealer purchased subscription of dealer by Admin API ")
    public ResponseEntity<ResponseDto<?>> getDealerPayments(
            @PathVariable Long Id) {

        return ResponseEntity.ok(paymentService.getDealerPayments(Id));
    }
}
