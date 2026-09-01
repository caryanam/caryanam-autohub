package com.autohub.service;

import com.autohub.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public interface DealerService {

    DealerResponseDTO registerDealer(DealerRegisterDTO dto, MultipartFile dealerLogo,MultipartFile showroomImage);

    String sendRegistrationOtp(String email);

    String verifyRegistrationOtp(String email, String otp);

    DealerResponseDTO getDealerProfile(Long dealerId);

    List<DealerSubscriptionResponseDTO> getSubscriptions();

    List<SubscriptionPlanDTO> getAllSubscriptionsPlans();

    DealerCurrentSubscriptionPlanDTO getDealerCurrentSubscriptionPlan(Long dealerId);

    DealerResponseDTO updateDealerAccountStatus(Long dealerId,DealerAccountStatusRequestDTO requestDTO);

    DealerProfileResponseDTO updateDealerProfile(Long id, UpdateDealerProfileRequestDTO dto, MultipartFile dealerLogo, MultipartFile showroomImage);

    DashboardResponseDTO getDashboard(Long dealerId);

    String sendWhatsappOtp(String whatsappNumber);

    String verifyWhatsappOtp(String whatsappNumber, String otp);
}