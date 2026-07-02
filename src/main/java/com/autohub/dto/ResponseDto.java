package com.autohub.dto;

import com.autohub.entity.Payment;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@JsonPropertyOrder({"status", "message", "data"})
public class ResponseDto<T> {

    private int status;
    private String message;
    private T data;

    public ResponseDto(boolean b, String error, Object data) {
    }



}