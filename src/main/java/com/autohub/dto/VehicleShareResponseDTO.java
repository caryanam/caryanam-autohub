package com.autohub.dto;
import com.autohub.enums.WhatsappDeliveryStatus;
import com.autohub.enums.WhatsappMessageStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class VehicleShareResponseDTO {

    private Long logId;
    private Long vehicleId;
    private String vehicleDisplayName;
    private String sentToNumber;
    private WhatsappMessageStatus status;
    private WhatsappDeliveryStatus deliveryStatus;
    private com.autohub.enums.ShareMode shareType;
    private String whatsappMessageId;
    private String message;
    
    @com.fasterxml.jackson.annotation.JsonProperty("createdAt")
    private LocalDateTime sharedAt;
}
