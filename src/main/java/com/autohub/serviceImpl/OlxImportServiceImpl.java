package com.autohub.serviceImpl;

import com.autohub.configuration.ZipExtractor;
import com.autohub.dto.CarImageResponse;
import com.autohub.dto.CarResponse;
import com.autohub.entity.Dealer;
import com.autohub.entity.Vehicle;
import com.autohub.entity.VehicleMedia;
import com.autohub.enums.VehicleStatus;
import com.autohub.enums.VehicleType;
import com.autohub.repository.DealerRepository;
import com.autohub.repository.VehicleRepository;
import com.autohub.service.OlxImportService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class OlxImportServiceImpl implements OlxImportService {

    private final VehicleRepository vehicleRepository;
    private final ZipExtractor zipExtractor;
    private final DealerRepository dealerRepository;

    @Value("${server.port}")
    private String port;

    @Override
    public void importData(MultipartFile excel,
                           MultipartFile zip) throws Exception {

        String uploadPath = "uploads/olx";

        zipExtractor.unzip(zip, uploadPath);

        try (Workbook workbook =
                     WorkbookFactory.create(excel.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            DataFormatter formatter = new DataFormatter();

            for (int rowNum = 1;
                 rowNum <= sheet.getLastRowNum();
                 rowNum++) {

                Row row = sheet.getRow(rowNum);

                if (isRowEmpty(row)) {
                    continue;
                }

                String carName =
                        getStringValue(formatter, row, 1);

                if (carName.isBlank()) {
                    continue;
                }

                Vehicle car = new Vehicle();

                Long dealerId = getLongValue(formatter, row, 0);

                Dealer dealer = dealerRepository.findById(dealerId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Dealer not found with id : " + dealerId));

                car.setDealer(dealer);

                car.setCity(
                        getStringValue(formatter, row, 1));

                car.setModel(
                        getStringValue(formatter, row, 2));

                car.setVariant(
                        getStringValue(formatter, row, 3));

                car.setBrand(
                        getStringValue(formatter, row, 4));

                car.setVehicleDescription(
                        getStringValue(formatter, row, 5));

                String fuelType =
                        getStringValue(formatter, row, 6);

                if (fuelType.length() > 100) {
                    fuelType = fuelType.substring(0, 100);
                }

                car.setFuelType(fuelType);

                car.setKilometerDriven(
                        getLongValue(formatter, row, 7));

                car.setAskingPrice(
                        getDoubleValue(formatter, row, 8));

                car.setOwnershipDetails(
                        getLongValue(formatter, row, 9).intValue());

                car.setRegistrationYear(
                        getLongValue(formatter, row, 10).intValue());

                car.setSubLocalityId(
                        getLongValue(formatter, row, 12));

                car.setDealerContactName(
                        getStringValue(formatter, row, 32));

                car.setDealerContactNumber(
                        getStringValue(formatter, row, 33));

                car.setFinanceAvailability(false);

                car.setCreatedAt(LocalDateTime.now());

                car.setVehicleType(VehicleType.NON_PREMIUM);

                car.setVehicleStatus(VehicleStatus.ACTIVE);

                int imageCount = 0;

                for (int col = 13; col <= 30; col++) {

                    String imageName =
                            "row_" + rowNum +
                                    "_col_" + col + ".jpg";

                    File imageFile =
                            new File(uploadPath
                                    + "/Images_Processed/"
                                    + imageName);

                    if (imageFile.exists()) {

                        VehicleMedia image =
                                new VehicleMedia();

                        System.out.println("Checking : " + imageFile.getAbsolutePath());
                        System.out.println("Exists : " + imageFile.exists());

                        image.setFileName(imageName);
                        image.setFileType("jpg"); // किंवा extension काढून टाक
                        image.setFilePath("/uploads/olx/Images_Processed/" + imageName);
                        image.setMediaType("IMAGE");
                        image.setUploadedAt(LocalDateTime.now());

                        image.setVehicle(car);

                        car.getMediaList().add(image);

                        imageCount++;

                    }
                }

                vehicleRepository.save(car);

                System.out.println(
                        "Imported Row : "
                                + rowNum
                                + " | Car : "
                                + carName
                                + " | Images : "
                                + imageCount);
            }
        }
    }

    @Override
    public CarResponse getCar(Long id) {

        Vehicle car = vehicleRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Car Not Found"));

        CarResponse response = new CarResponse();

        response.setId(car.getId());
        response.setBrand(car.getBrand());
        response.setCarName(car.getVariant());
        response.setPrice(car.getAskingPrice());

        List<CarImageResponse> images =
                car.getMediaList()
                        .stream()
                        .map(image -> {

                            CarImageResponse dto =
                                    new CarImageResponse();

                            dto.setId(image.getId());
                            dto.setImageUrl(
                                    "http://localhost:" + port +
                                            image.getFilePath().replace("\\", "/")
                            );

                            return dto;

                        }).toList();

        response.setImages(images);

        return response;
    }

    private boolean isRowEmpty(Row row) {

        if (row == null) {
            return true;
        }

        DataFormatter formatter =
                new DataFormatter();

        for (int i = row.getFirstCellNum();
             i < row.getLastCellNum();
             i++) {

            Cell cell = row.getCell(i);

            if (cell != null &&
                    !formatter.formatCellValue(cell)
                            .trim()
                            .isEmpty()) {

                return false;
            }
        }

        return true;
    }

    private String getStringValue(
            DataFormatter formatter,
            Row row,
            int index) {

        Cell cell = row.getCell(index);

        if (cell == null) {
            return "";
        }

        return formatter
                .formatCellValue(cell)
                .trim();
    }

    private Long getLongValue(
            DataFormatter formatter,
            Row row,
            int index) {

        try {

            String value =
                    getStringValue(
                            formatter,
                            row,
                            index);

            if (value.isBlank()) {
                return 0L;
            }

            return Long.parseLong(
                    value.replace(".0", ""));

        } catch (Exception e) {
            return 0L;
        }
    }

    private Double getDoubleValue(
            DataFormatter formatter,
            Row row,
            int index) {

        try {

            String value =
                    getStringValue(
                            formatter,
                            row,
                            index);

            if (value.isBlank()) {
                return 0D;
            }

            value = value.replace(",", "");

            return Double.parseDouble(value);

        } catch (Exception e) {
            return 0D;
        }
    }


    @Override
    public List<CarResponse> getAllCars() {

        List<Vehicle> cars = vehicleRepository.findAll();

        return cars.stream()
                .map(car -> {

                    CarResponse response =
                            new CarResponse();

                    response.setId(car.getId());
                    response.setBrand(car.getBrand());
                    response.setCarName(car.getVariant());
                    response.setPrice(car.getAskingPrice());

                    List<CarImageResponse> images =
                            car.getMediaList()
                                    .stream()
                                    .map(image -> {

                                        CarImageResponse dto =
                                                new CarImageResponse();

                                        dto.setId(image.getId());
                                       // dto.setImageUrl( image.getImageUrl());
                                        dto.setImageUrl(
                                                "http://localhost:" + port +
                                                        image.getFilePath().replace("\\", "/")
                                        );
                                        return dto;

                                    }).toList();

                    response.setImages(images);

                    return response;

                }).toList();
    }
}