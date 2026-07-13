package com.autohub.service;

import com.autohub.dto.DealerOfferResponseDTO;
import com.autohub.dto.offer.DealerOfferRequestDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface DealerOfferService {

    DealerOfferResponseDTO sendOfferToAllDealers(
            DealerOfferRequestDTO requestDTO,
            MultipartFile offerImage,
            Long adminId);

    // Returns all past offers with per-dealer delivery breakdown
    List<DealerOfferResponseDTO> getAllOffers();

    // Returns single offer detail with full dealer log
    DealerOfferResponseDTO getOfferById(Long offerId);
}
