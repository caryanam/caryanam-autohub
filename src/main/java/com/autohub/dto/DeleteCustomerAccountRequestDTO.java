package com.autohub.dto;

import lombok.Data;

@Data
public class DeleteCustomerAccountRequestDTO {

    private String username;   // Email OR Mobile
    private String password;
}
