package com.autohub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AreaResponseDTO {

    private String city;
    private String area;
    private String pincode;
    private String nearBy;
}
