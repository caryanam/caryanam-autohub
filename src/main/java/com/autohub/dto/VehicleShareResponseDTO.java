package com.autohub.dto;
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
    private com.autohub.enums.ShareMode shareType;
    private String whatsappMessageId;
    private String message;
    private LocalDateTime sharedAt;
}
