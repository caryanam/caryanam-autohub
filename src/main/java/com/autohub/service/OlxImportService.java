package com.autohub.service;

import com.autohub.dto.CarResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OlxImportService {

    void importData(
            MultipartFile excel,
            MultipartFile zip)
            throws Exception;

    CarResponse getCar(Long id);

    List<CarResponse> getAllCars();
}
