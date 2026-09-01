package com.autohub.service;

import com.autohub.dto.SocialVisitLogDTO;
import com.autohub.dto.RecordSocialVisitRequestDTO;

import java.util.List;

public interface SocialVisitService {
    void recordVisit(RecordSocialVisitRequestDTO request);
    List<SocialVisitLogDTO> getAllVisits();
    List<SocialVisitLogDTO> getVisitsByVehicle(Long vehicleId);
    List<SocialVisitLogDTO> getVisitsByDealer(Long dealerId);
}
