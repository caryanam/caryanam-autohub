package com.autohub.service;


import com.autohub.dto.PaymentRequestDTO;
import com.autohub.dto.ResponseDto;

public interface PaymentService {

    ResponseDto createPayment(PaymentRequestDTO dto);

    ResponseDto paymentSuccess(Long paymentId);

    ResponseDto paymentFailed(Long paymentId);
    ResponseDto<?> getAllPayments();
    ResponseDto<?> getDealerPayments(Long Id);
}

