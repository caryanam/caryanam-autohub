package com.autohub.dto;

import com.autohub.entity.FestivalOffer.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FestivalOfferResponseDTO {

    private Long id;
    private String mediaUrl;
    private MediaType mediaType;
    private Boolean enabled;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
