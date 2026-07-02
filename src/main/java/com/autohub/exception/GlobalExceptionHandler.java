package com.autohub.exception;

import com.autohub.dto.ResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {

        Map<String, Object> error = new HashMap<>();
        error.put("status", 500);
        error.put("message", ex.getMessage());

        return ResponseEntity.internalServerError().body(error);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new LinkedHashMap<>();

        ex.getBindingResult()
                .getFieldErrors()
                .forEach(error ->
                        errors.put(error.getField(),
                                error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(
                Map.of(
                        "status", 400,
                        "message", "Validation Failed",
                        "errors", errors
                )
        );
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ResponseDto> handleResourceNotFoundException(ResourceNotFoundException e) {
        return new ResponseEntity<>(new ResponseDto(404, e.getMessage(), null), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(WishlistAlreadyExistsException.class)
    public ResponseEntity<ResponseDto> handleWishlistAlreadyExists(WishlistAlreadyExistsException ex ) {

        return new ResponseEntity<>(new ResponseDto(409, ex.getMessage(), null), HttpStatus.CONFLICT);
    }
}