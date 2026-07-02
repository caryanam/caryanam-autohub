package com.autohub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatPrincipal {

    private Long id;

    private String role;

    private String email;
}
