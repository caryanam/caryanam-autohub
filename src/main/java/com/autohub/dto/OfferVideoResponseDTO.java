package com.autohub.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OfferVideoResponseDTO {

    private Long id;

    private String videoUrl;       // full absolute URL (e.g. https://c1.caryanam.com/uploads/offer-videos/xxx.mp4)
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
