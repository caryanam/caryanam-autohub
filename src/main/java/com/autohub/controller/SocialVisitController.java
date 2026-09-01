package com.autohub.controller;

import com.autohub.dto.RecordSocialVisitRequestDTO;
import com.autohub.dto.ResponseDto;
import com.autohub.service.SocialVisitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/social-tracking")
@RequiredArgsConstructor
public class SocialVisitController {

    private final SocialVisitService visitService;

    @PostMapping("/visit")
    public ResponseEntity<ResponseDto<String>> recordVisit(@RequestBody RecordSocialVisitRequestDTO request) {
        visitService.recordVisit(request);
        return ResponseEntity.ok(new ResponseDto<>(200, "Visit recorded successfully", null));
    }
}
