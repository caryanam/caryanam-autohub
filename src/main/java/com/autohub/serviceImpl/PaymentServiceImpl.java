package com.autohub.serviceImpl;

import com.autohub.dto.PaymentRequestDTO;
import com.autohub.dto.PaymentResponseDTO;
import com.autohub.dto.ResponseDto;
import com.autohub.entity.Dealer;
import com.autohub.entity.Payment;
import com.autohub.enums.PaymentStatus;
import com.autohub.repository.DealerRepository;
import com.autohub.repository.PaymentRepository;
import com.autohub.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final DealerRepository dealerRepository;

    @Override
    public ResponseDto<?> createPayment(PaymentRequestDTO dto) {

        // Dealer fetch
        Dealer dealer = dealerRepository.findById(dto.getDealerId())
                .orElseThrow(() -> new RuntimeException("Dealer not found"));

        // Validation 1 - Active subscription
        if (Boolean.TRUE.equals(dealer.getSubscriptionActive())) {
            throw new RuntimeException(
                    "You have already an active subscription plan");
        }

        // Validation 2 - Pending payment exists
        Optional<Payment> pendingPayment =
                paymentRepository
                        .findTopByDealerIdAndPaymentStatusOrderByPaymentIdDesc(
                                dto.getDealerId(),
                                PaymentStatus.PENDING);

        if (pendingPayment.isPresent()) {
            throw new RuntimeException(
                    "You have already subscription plan but Your subscription plan is waiting for admin approval.");
        }

        // Payment create logic
        Payment payment = new Payment();
        payment.setDealer(dealer);
        payment.setSubscriptionPlan(dto.getSubscriptionPlan());
        payment.setAmount(dto.getSubscriptionPlan().getAmount());
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setPaymentStatus((PaymentStatus.PENDING));

        Payment savedPayment = paymentRepository.save(payment);




        return new ResponseDto<>(
                200,
                "Payment Created Successfully",
                PaymentResponseDTO.builder()
                        .paymentId(savedPayment.getPaymentId())
                        .dealer(savedPayment.getDealer())
                        .businessName(savedPayment.getDealer().getBusinessName())
                        .subscriptionPlan(savedPayment.getSubscriptionPlan())
                        .amount(savedPayment.getAmount())
                        .transactionId(savedPayment.getTransactionId())
                        .paymentStatus(savedPayment.getPaymentStatus())
                        .paymentDate(savedPayment.getPaymentDate())
                        .build()
        );
    }

    @Override
    public ResponseDto<?> paymentSuccess(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            return new ResponseDto<>(
                    400,
                    "Payment already completed",
                    null
            );
        }

        // Update payment
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        payment.setPaymentDate(LocalDateTime.now());

        // Dealer
        Dealer dealer = payment.getDealer();

        dealer.setSubscriptionActive(true);
        dealer.setSubscriptionPlan(payment.getSubscriptionPlan());
        dealer.setSubscriptionStartDate(LocalDate.now().atStartOfDay());
        dealer.setSubscriptionEndDate(
                LocalDate.now().plusMonths(1).atStartOfDay()
        );

        dealerRepository.save(dealer);
        paymentRepository.save(payment);

        return new ResponseDto<>(
                200,
                "Payment Successful",
                payment
        );
    }


    @Override
    public ResponseDto paymentFailed(Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new RuntimeException("Payment not found"));

        payment.setPaymentStatus(PaymentStatus.valueOf("FAILED"));

        paymentRepository.save(payment);

        return new ResponseDto<>(
                400,
                "Payment Failed",
                null
        );
    }


    @Override
    public ResponseDto<?> getAllPayments() {

        List<Payment> payments =
                paymentRepository.findAll();

        return new ResponseDto<>(
                200,
                "All Payments Fetched Successfully",
                payments
        );
    }


    @Override
    public ResponseDto<?> getDealerPayments(
            Long Id) {

        List<Payment> payments =
                paymentRepository
                        .findByDealerId(String.valueOf(Id));

        return new ResponseDto<>(
                200,
                "Dealer Payment History Fetched Successfully",
                payments
        );
    }
}