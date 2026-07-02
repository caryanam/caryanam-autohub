package com.autohub.controller;

import com.autohub.dto.CarResponse;
import com.autohub.service.DealerImportService;
import com.autohub.service.OlxImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/olx")
@RequiredArgsConstructor
public class OlxImportController {

    private final OlxImportService service;

    private final DealerImportService dealerImportService;


    //Import Vehicle Data
    @PostMapping(
            value = "/import",
            consumes =
                    MediaType.MULTIPART_FORM_DATA_VALUE)
    public String importExcel(
            @RequestParam MultipartFile excel,
            @RequestParam MultipartFile zip)
            throws Exception {

        service.importData(excel, zip);

        return "Imported Successfully";
    }

    //Get Vehicle By id
    @GetMapping("/vehicle/{id}")
    public CarResponse getCar(
            @PathVariable Long id) {

        return service.getCar(id);
    }

    //Get All imported Vehicle
    @GetMapping("/all-vehicle")
    public List<CarResponse> getAllCars() {

        return service.getAllCars();
    }


    //Import Dealer Registration
    @PostMapping(
            value = "/import-dealer",
            consumes =
                    MediaType.MULTIPART_FORM_DATA_VALUE)
    public String importDealer(
            @RequestParam MultipartFile excel)
            throws Exception {

        dealerImportService
                .importDealerData(excel);

        return "Dealer Imported Successfully";
    }

}
