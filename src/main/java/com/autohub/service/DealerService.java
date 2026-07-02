package com.autohub.service;

import com.autohub.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Service
public interface DealerService {

    DealerResponseDTO registerDealer(DealerRegisterDTO dto, MultipartFile dealerLogo,MultipartFile showroomImage);

    DealerResponseDTO getDealerProfile(Long dealerId);

    List<DealerSubscriptionResponseDTO> getSubscriptions();

    List<SubscriptionPlanDTO> getAllSubscriptionsPlans();

    DealerCurrentSubscriptionPlanDTO getDealerCurrentSubscriptionPlan(Long dealerId);

    DealerResponseDTO updateDealerAccountStatus(Long dealerId,DealerAccountStatusRequestDTO requestDTO);

    DealerProfileResponseDTO updateDealerProfile(Long id, UpdateDealerProfileRequestDTO dto);

    DashboardResponseDTO getDashboard(Long dealerId);


}