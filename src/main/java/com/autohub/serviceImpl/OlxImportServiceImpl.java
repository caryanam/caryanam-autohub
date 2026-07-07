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
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;

@Service
@RequiredArgsConstructor
@Transactional
public class OlxImportServiceImpl implements OlxImportService {

    private final VehicleRepository vehicleRepository;
    private final ZipExtractor zipExtractor;
    private final DealerRepository dealerRepository;

    @Value("${server.port}")
    private String port;
    @Value("${spring.server.url}")
    private String serverUrl;


    @Override
    public void importData(MultipartFile excel,
                           MultipartFile zip) throws Exception {

        String uploadPath = "uploads/olx";

        File imageDir = new File(uploadPath + "/Images_Processed");

        if (imageDir.exists()) {
            FileSystemUtils.deleteRecursively(imageDir);
            System.out.println("Cleared stale image directory before extracting new zip.");
        }

        zipExtractor.unzip(zip, uploadPath);


        Map<Integer, List<File>> rowImagesMap = new HashMap<>();

        System.out.println("Image Dir Exists : " + imageDir.exists());
        System.out.println("Image Dir Path   : " + imageDir.getAbsolutePath());

        if (imageDir.exists() && imageDir.isDirectory()) {

            File[] allImages = imageDir.listFiles();

            System.out.println("Total Files Found : "
                    + (allImages == null ? 0 : allImages.length));

            if (allImages != null) {

                Pattern pattern = Pattern.compile(
                        "row_(\\d+)_col_(\\d+)\\.(jpg|jpeg|png)",
                        Pattern.CASE_INSENSITIVE
                );

                for (File file : allImages) {

                    Matcher matcher = pattern.matcher(file.getName());

                    if (matcher.matches()) {

                        Integer imageRowKey = Integer.parseInt(matcher.group(1));

                        rowImagesMap
                                .computeIfAbsent(imageRowKey, k -> new ArrayList<>())
                                .add(file);
                    } else {
                        System.out.println(
                                "WARNING: file does not match row_X_col_Y pattern, skipped: "
                                        + file.getName());
                    }
                }
            }
        }

        int totalImagesOnDisk = rowImagesMap.values().stream()
                .mapToInt(List::size)
                .sum();

        System.out.println("Total Images Found : " + totalImagesOnDisk);

        List<Integer> sortedImageRowKeys = new ArrayList<>(rowImagesMap.keySet());
        Collections.sort(sortedImageRowKeys);

        System.out.println("Distinct image row groups found : " + sortedImageRowKeys.size());

        int imageGroupPointer = 0;

        int rowsProcessed = 0;
        int rowsSucceeded = 0;
        int totalImagesSaved = 0;
        List<String> failures = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(excel.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            DataFormatter formatter = new DataFormatter();

            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {

                Row row = sheet.getRow(rowNum);

                if (isRowEmpty(row)) {
                    continue;
                }

                String carName = getStringValue(formatter, row, 2);

                if (carName.isBlank()) {
                    continue;
                }

                rowsProcessed++;

                List<File> vehicleImages = null;
                Integer resolvedImageRowKey = null;

                if (imageGroupPointer < sortedImageRowKeys.size()) {
                    resolvedImageRowKey = sortedImageRowKeys.get(imageGroupPointer);
                    vehicleImages = rowImagesMap.get(resolvedImageRowKey);
                } else {
                    System.out.println(
                            "WARNING: ran out of image groups at Excel row " + rowNum);
                }

                imageGroupPointer++;

                try {

                    Long dealerId = getLongValue(formatter, row, 0);

                    Dealer dealer = dealerRepository.findById(dealerId)
                            .orElseThrow(() -> new RuntimeException(
                                    "Dealer not found : " + dealerId));

                    Vehicle car = new Vehicle();

                    car.setDealer(dealer);
                    car.setCity(getStringValue(formatter, row, 1));
                    car.setModel(getStringValue(formatter, row, 2));
                    car.setVariant(getStringValue(formatter, row, 3));
                    car.setBrand(getStringValue(formatter, row, 4));
                    car.setVehicleDescription(getStringValue(formatter, row, 5));

                    String fuelType = getStringValue(formatter, row, 6);

                    if (fuelType.length() > 100) {
                        fuelType = fuelType.substring(0, 100);
                    }

                    car.setFuelType(fuelType);
                    car.setKilometerDriven(getLongValue(formatter, row, 7));
                    car.setAskingPrice(getDoubleValue(formatter, row, 8));
                    car.setOwnershipDetails(getLongValue(formatter, row, 9).intValue());
                    car.setRegistrationYear(getLongValue(formatter, row, 10).intValue());
                    car.setDealerContactName(getStringValue(formatter, row, 29));
                    car.setDealerContactNumber(getStringValue(formatter, row, 30));

                    car.setFinanceAvailability(false);
                    car.setVehicleType(VehicleType.NON_PREMIUM);
                    car.setVehicleStatus(VehicleStatus.ACTIVE);
                    car.setCreatedAt(LocalDateTime.now());

                    int imageCount = 0;

                    if (vehicleImages != null) {

                        vehicleImages.sort(Comparator.comparingInt(this::extractColNumber));

                        for (File imageFile : vehicleImages) {

                            String imageName = imageFile.getName();

                            String extension =
                                    imageName.substring(imageName.lastIndexOf(".") + 1);

                            VehicleMedia image = new VehicleMedia();

                            image.setFileName(imageName);
                            image.setFileType(extension);
                            image.setFilePath(
                                    "/uploads/olx/Images_Processed/" + imageName);
                            image.setMediaType("IMAGE");
                            image.setUploadedAt(LocalDateTime.now());
                            image.setVehicle(car);

                            car.getMediaList().add(image);

                            imageCount++;
                        }
                    }

                    vehicleRepository.save(car);

                    rowsSucceeded++;
                    totalImagesSaved += imageCount;

                    System.out.println(
                            "Imported Row : "
                                    + rowNum
                                    + " | Image Group Key (row_X) : "
                                    + resolvedImageRowKey
                                    + " | Vehicle : "
                                    + car.getBrand()
                                    + " "
                                    + car.getModel()
                                    + " | Images : "
                                    + imageCount);

                } catch (Exception rowEx) {

                    String msg = "Row " + rowNum
                            + " (image group " + resolvedImageRowKey + ") FAILED: "
                            + rowEx.getMessage();

                    System.out.println("ERROR: " + msg);
                    failures.add(msg);
                }
            }
        }

        // ------------------------------------------------------------------
        // STEP 3: End-of-run summary so gaps are visible instead of silent.
        // ------------------------------------------------------------------
        System.out.println("==================== IMPORT SUMMARY ====================");
        System.out.println("Excel data rows processed : " + rowsProcessed);
        System.out.println("Vehicles saved            : " + rowsSucceeded);
        System.out.println("Vehicles failed            : " + failures.size());
        System.out.println("Images found on disk       : " + totalImagesOnDisk);
        System.out.println("Images saved to DB         : " + totalImagesSaved);

        if (imageGroupPointer < sortedImageRowKeys.size()) {
            System.out.println(
                    "WARNING: " + (sortedImageRowKeys.size() - imageGroupPointer)
                            + " image group(s) in the zip were never consumed "
                            + "(more image groups than valid Excel rows).");
        }

        if (!failures.isEmpty()) {
            System.out.println("---- Failed rows ----");
            failures.forEach(System.out::println);
        }
        System.out.println("=========================================================");
    }

    /**
     * Extracts the numeric N from a filename like "row_6_col_13.jpg" -> 13.
     * Used to sort a vehicle's images in the correct left-to-right order.
     */
    private int extractColNumber(File imageFile) {

        Matcher matcher = Pattern
                .compile("row_(\\d+)_col_(\\d+)\\.(jpg|jpeg|png)", Pattern.CASE_INSENSITIVE)
                .matcher(imageFile.getName());

        if (matcher.matches()) {
            return Integer.parseInt(matcher.group(2));
        }

        return Integer.MAX_VALUE; // shouldn't happen, push unmatched files to the end
    }














//    @Override
//    public void importData(MultipartFile excel,
//                           MultipartFile zip) throws Exception {
//
//        String uploadPath = "uploads/olx";
//
//        zipExtractor.unzip(zip, uploadPath);
//
//        // ------------------------------------------------------------------
//        // STEP 1: Scan all images once and group them by their "row_X" number.
//        // ------------------------------------------------------------------
//        // The "row_X" number in the filename is NOT the Excel row number - it's
//        // a leftover index from the original scrape, before rows were filtered
//        // into this processed Excel file. What lines up is ORDER: the smallest
//        // row_X group belongs to the first Excel data row, the second-smallest
//        // to the second data row, and so on. So we sort the distinct row_X
//        // values ascending and consume them in lockstep with Excel rows.
//
//        Map<Integer, List<File>> rowImagesMap = new HashMap<>();
//
//        File imageDir = new File(uploadPath + "/Images_Processed");
//
//        System.out.println("Image Dir Exists : " + imageDir.exists());
//        System.out.println("Image Dir Path   : " + imageDir.getAbsolutePath());
//
//        if (imageDir.exists() && imageDir.isDirectory()) {
//
//            File[] allImages = imageDir.listFiles();
//
//            System.out.println("Total Files Found : "
//                    + (allImages == null ? 0 : allImages.length));
//
//            if (allImages != null) {
//
//                Pattern pattern = Pattern.compile(
//                        "row_(\\d+)_col_(\\d+)\\.(jpg|jpeg|png)",
//                        Pattern.CASE_INSENSITIVE
//                );
//
//                for (File file : allImages) {
//
//                    Matcher matcher = pattern.matcher(file.getName());
//
//                    if (matcher.matches()) {
//
//                        Integer imageRowKey = Integer.parseInt(matcher.group(1));
//
//                        rowImagesMap
//                                .computeIfAbsent(imageRowKey, k -> new ArrayList<>())
//                                .add(file);
//                    } else {
//                        // Anything that doesn't match the expected pattern is
//                        // silently ignored today - log it so it's visible.
//                        System.out.println(
//                                "WARNING: file does not match row_X_col_Y pattern, skipped: "
//                                        + file.getName());
//                    }
//                }
//            }
//        }
//
//        int totalImagesOnDisk = rowImagesMap.values().stream()
//                .mapToInt(List::size)
//                .sum();
//
//        System.out.println("Total Images Found : " + totalImagesOnDisk);
//
//        List<Integer> sortedImageRowKeys = new ArrayList<>(rowImagesMap.keySet());
//        Collections.sort(sortedImageRowKeys);
//
//        System.out.println("Distinct image row groups found : " + sortedImageRowKeys.size());
//
//        int imageGroupPointer = 0;
//
//        // Track what happened so a bad row doesn't silently swallow the rest
//        // of the import, and so you get a clear report at the end.
//        int rowsProcessed = 0;
//        int rowsSucceeded = 0;
//        int totalImagesSaved = 0;
//        List<String> failures = new ArrayList<>();
//
//        // ------------------------------------------------------------------
//        // STEP 2: Walk the Excel rows and consume image groups in lockstep.
//        // ------------------------------------------------------------------
//        try (Workbook workbook = WorkbookFactory.create(excel.getInputStream())) {
//
//            Sheet sheet = workbook.getSheetAt(0);
//
//            DataFormatter formatter = new DataFormatter();
//
//            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
//
//                Row row = sheet.getRow(rowNum);
//
//                if (isRowEmpty(row)) {
//                    continue;
//                }
//
//                String carName = getStringValue(formatter, row, 2);
//
//                if (carName.isBlank()) {
//                    continue;
//                }
//
//                rowsProcessed++;
//
//                // Resolve this row's image group up front (pointer must advance
//                // exactly once per data row regardless of success/failure below,
//                // otherwise every row after a failure would shift out of sync).
//                List<File> vehicleImages = null;
//                Integer resolvedImageRowKey = null;
//
//                if (imageGroupPointer < sortedImageRowKeys.size()) {
//                    resolvedImageRowKey = sortedImageRowKeys.get(imageGroupPointer);
//                    vehicleImages = rowImagesMap.get(resolvedImageRowKey);
//                } else {
//                    System.out.println(
//                            "WARNING: ran out of image groups at Excel row " + rowNum);
//                }
//
//                imageGroupPointer++;
//
//                // ------------------------------------------------------------
//                // Isolate this row: any exception here is caught, logged, and
//                // the loop moves on - so ONE bad row (bad dealer id, bad number
//                // format, unreadable image, etc.) can no longer wipe out every
//                // row that comes after it. This is the actual fix for the
//                // "1796 files but only 1000 saved" gap.
//                // ------------------------------------------------------------
//                try {
//
//                    Long dealerId = getLongValue(formatter, row, 0);
//
//                    Dealer dealer = dealerRepository.findById(dealerId)
//                            .orElseThrow(() -> new RuntimeException(
//                                    "Dealer not found : " + dealerId));
//
//                    Vehicle car = new Vehicle();
//
//                    car.setDealer(dealer);
//                    car.setCity(getStringValue(formatter, row, 1));
//                    car.setModel(getStringValue(formatter, row, 2));
//                    car.setVariant(getStringValue(formatter, row, 3));
//                    car.setBrand(getStringValue(formatter, row, 4));
//                    car.setVehicleDescription(getStringValue(formatter, row, 5));
//
//                    String fuelType = getStringValue(formatter, row, 6);
//
//                    if (fuelType.length() > 100) {
//                        fuelType = fuelType.substring(0, 100);
//                    }
//
//                    car.setFuelType(fuelType);
//                    car.setKilometerDriven(getLongValue(formatter, row, 7));
//                    car.setAskingPrice(getDoubleValue(formatter, row, 8));
//                    car.setOwnershipDetails(getLongValue(formatter, row, 9).intValue());
//                    car.setRegistrationYear(getLongValue(formatter, row, 10).intValue());
//                    car.setDealerContactName(getStringValue(formatter, row, 29));
//                    car.setDealerContactNumber(getStringValue(formatter, row, 30));
//
//                    car.setFinanceAvailability(false);
//                    car.setVehicleType(VehicleType.NON_PREMIUM);
//                    car.setVehicleStatus(VehicleStatus.ACTIVE);
//                    car.setCreatedAt(LocalDateTime.now());
//
//                    int imageCount = 0;
//
//                    if (vehicleImages != null) {
//
//                        // Sort NUMERICALLY by col_N, not lexicographically by
//                        // filename (lexicographic would put col_10 before col_2).
//                        vehicleImages.sort(Comparator.comparingInt(this::extractColNumber));
//
//                        for (File imageFile : vehicleImages) {
//
//                            String imageName = imageFile.getName();
//
//                            String extension =
//                                    imageName.substring(imageName.lastIndexOf(".") + 1);
//
//                            VehicleMedia image = new VehicleMedia();
//
//                            image.setFileName(imageName);
//                            image.setFileType(extension);
//                            image.setFilePath(
//                                    "/uploads/olx/Images_Processed/" + imageName);
//                            image.setMediaType("IMAGE");
//                            image.setUploadedAt(LocalDateTime.now());
//                            image.setVehicle(car);
//
//                            car.getMediaList().add(image);
//
//                            imageCount++;
//                        }
//                    }
//
//                    vehicleRepository.save(car);
//
//                    rowsSucceeded++;
//                    totalImagesSaved += imageCount;
//
//                    System.out.println(
//                            "Imported Row : "
//                                    + rowNum
//                                    + " | Image Group Key (row_X) : "
//                                    + resolvedImageRowKey
//                                    + " | Vehicle : "
//                                    + car.getBrand()
//                                    + " "
//                                    + car.getModel()
//                                    + " | Images : "
//                                    + imageCount);
//
//                } catch (Exception rowEx) {
//
//                    String msg = "Row " + rowNum
//                            + " (image group " + resolvedImageRowKey + ") FAILED: "
//                            + rowEx.getMessage();
//
//                    System.out.println("ERROR: " + msg);
//                    failures.add(msg);
//                    // Continue to the next row instead of aborting the whole import.
//                }
//            }
//        }
//
//        // ------------------------------------------------------------------
//        // STEP 3: End-of-run summary so gaps are visible instead of silent.
//        // ------------------------------------------------------------------
//        System.out.println("==================== IMPORT SUMMARY ====================");
//        System.out.println("Excel data rows processed : " + rowsProcessed);
//        System.out.println("Vehicles saved            : " + rowsSucceeded);
//        System.out.println("Vehicles failed            : " + failures.size());
//        System.out.println("Images found on disk       : " + totalImagesOnDisk);
//        System.out.println("Images saved to DB         : " + totalImagesSaved);
//
//        if (imageGroupPointer < sortedImageRowKeys.size()) {
//            System.out.println(
//                    "WARNING: " + (sortedImageRowKeys.size() - imageGroupPointer)
//                            + " image group(s) in the zip were never consumed "
//                            + "(more image groups than valid Excel rows).");
//        }
//
//        if (!failures.isEmpty()) {
//            System.out.println("---- Failed rows ----");
//            failures.forEach(System.out::println);
//        }
//        System.out.println("=========================================================");
//    }
//
//    /**
//     * Extracts the numeric N from a filename like "row_6_col_13.jpg" -> 13.
//     * Used to sort a vehicle's images in the correct left-to-right order.
//     */
//    private int extractColNumber(File imageFile) {
//
//        Matcher matcher = Pattern
//                .compile("row_(\\d+)_col_(\\d+)\\.(jpg|jpeg|png)", Pattern.CASE_INSENSITIVE)
//                .matcher(imageFile.getName());
//
//        if (matcher.matches()) {
//            return Integer.parseInt(matcher.group(2));
//        }
//
//        return Integer.MAX_VALUE; // shouldn't happen, push unmatched files to the end
//    }











//
//    @Override
//    public void importData(MultipartFile excel,
//                           MultipartFile zip) throws Exception {
//
//        String uploadPath = "uploads/olx";
//
//        zipExtractor.unzip(zip, uploadPath);
//
//        // Scan all images once
//        Map<Integer, List<File>> rowImagesMap = new HashMap<>();
//
//
//
//        File imageDir =
//                new File(uploadPath + "/Images_Processed");
//
//        System.out.println("Image Dir Exists : " + imageDir.exists());
//        System.out.println("Image Dir Path   : " + imageDir.getAbsolutePath());
//
//
//
//        if (imageDir.exists() && imageDir.isDirectory()) {
//
//            File[] allImages = imageDir.listFiles();
//
//            System.out.println("Total Files Found : "
//                    + (allImages == null ? 0 : allImages.length));
//
//            if (allImages != null) {
//
//                Pattern pattern = Pattern.compile(
//                        "row_(\\d+)_col_(\\d+)\\.(jpg|jpeg|png)",
//                        Pattern.CASE_INSENSITIVE
//                );
//
//                for (File file : allImages) {
//
//                    Matcher matcher =
//                            pattern.matcher(file.getName());
//
//                    if (matcher.matches()) {
//
//                        Integer excelRow =
//                                Integer.parseInt(
//                                        matcher.group(1));
//
//                        rowImagesMap
//                                .computeIfAbsent(
//                                        excelRow,
//                                        k -> new ArrayList<>())
//                                .add(file);
//                    }
//                }
//            }
//        }
//
//        System.out.println(
//                "Total Images Found : "
//                        + rowImagesMap.values()
//                        .stream()
//                        .mapToInt(List::size)
//                        .sum());
//
//        try (Workbook workbook =
//                     WorkbookFactory.create(excel.getInputStream())) {
//
//            Sheet sheet = workbook.getSheetAt(0);
//
//            DataFormatter formatter =
//                    new DataFormatter();
//
//            for (int rowNum = 1;
//                 rowNum <= sheet.getLastRowNum();
//                 rowNum++) {
//
//                Row row = sheet.getRow(rowNum);
//
//                if (isRowEmpty(row)) {
//                    continue;
//                }
//
//                String carName =
//                        getStringValue(
//                                formatter,
//                                row,
//                                2);
//
//                if (carName.isBlank()) {
//                    continue;
//                }
//
//                Long dealerId =
//                        getLongValue(
//                                formatter,
//                                row,
//                                0);
//
//                Dealer dealer =
//                        dealerRepository.findById(
//                                        dealerId)
//                                .orElseThrow(() ->
//                                        new RuntimeException(
//                                                "Dealer not found : "
//                                                        + dealerId));
//
//                Vehicle car = new Vehicle();
//
//                car.setDealer(dealer);
//                car.setCity(
//                        getStringValue(
//                                formatter,
//                                row,
//                                1));
//
//                car.setModel(
//                        getStringValue(
//                                formatter,
//                                row,
//                                2));
//
//                car.setVariant(
//                        getStringValue(
//                                formatter,
//                                row,
//                                3));
//
//                car.setBrand(
//                        getStringValue(
//                                formatter,
//                                row,
//                                4));
//
//                car.setVehicleDescription(
//                        getStringValue(
//                                formatter,
//                                row,
//                                5));
//
//                String fuelType =
//                        getStringValue(
//                                formatter,
//                                row,
//                                6);
//
//                if (fuelType.length() > 100) {
//                    fuelType =
//                            fuelType.substring(
//                                    0,
//                                    100);
//                }
//
//                car.setFuelType(fuelType);
//
//                car.setKilometerDriven(
//                        getLongValue(
//                                formatter,
//                                row,
//                                7));
//
//                car.setAskingPrice(
//                        getDoubleValue(
//                                formatter,
//                                row,
//                                8));
//
//                car.setOwnershipDetails(
//                        getLongValue(
//                                formatter,
//                                row,
//                                9).intValue());
//
//                car.setRegistrationYear(
//                        getLongValue(
//                                formatter,
//                                row,
//                                10).intValue());
//
//                car.setDealerContactName(
//                        getStringValue(
//                                formatter,
//                                row,
//                                29));
//
//                car.setDealerContactNumber(
//                        getStringValue(
//                                formatter,
//                                row,
//                                30));
//
//                car.setFinanceAvailability(false);
//                car.setVehicleType(
//                        VehicleType.NON_PREMIUM);
//
//                car.setVehicleStatus(
//                        VehicleStatus.ACTIVE);
//
//                car.setCreatedAt(
//                        LocalDateTime.now());
//
//                // IMPORTANT
//                // Screenshot मध्ये row_6 पासून images सुरू आहेत
//                int imageRowNumber = rowNum + 1;
//
//                List<File> vehicleImages =
//                        rowImagesMap.get(
//                                imageRowNumber);
//
//                int imageCount = 0;
//
//                if (vehicleImages != null) {
//
//                    vehicleImages.sort(
//                            Comparator.comparing(
//                                    File::getName));
//
//                    for (File imageFile : vehicleImages) {
//
//                        String imageName =
//                                imageFile.getName();
//
//                        String extension =
//                                imageName.substring(
//                                        imageName.lastIndexOf(".")
//                                                + 1);
//
//                        VehicleMedia image =
//                                new VehicleMedia();
//
//                        image.setFileName(
//                                imageName);
//
//                        image.setFileType(
//                                extension);
//
//                        image.setFilePath(
//                                "/uploads/olx/Images_Processed/"
//                                        + imageName);
//
//                        image.setMediaType(
//                                "IMAGE");
//
//                        image.setUploadedAt(
//                                LocalDateTime.now());
//
//                        image.setVehicle(car);
//
//                        car.getMediaList()
//                                .add(image);
//
//                        imageCount++;
//                    }
//                }
//
//                vehicleRepository.save(car);
//
//                System.out.println(
//                        "Imported Row : "
//                                + rowNum
//                                + " | Image Row : "
//                                + imageRowNumber
//                                + " | Vehicle : "
//                                + car.getBrand()
//                                + " "
//                                + car.getModel()
//                                + " | Images : "
//                                + imageCount);
//            }
//        }
//    }

//    @Override
//    public void importData(MultipartFile excel,
//                           MultipartFile zip) throws Exception {
//
//        String uploadPath = "uploads/olx";
//
//        zipExtractor.unzip(zip, uploadPath);
//
//        try (Workbook workbook =
//                     WorkbookFactory.create(excel.getInputStream())) {
//
//            Sheet sheet = workbook.getSheetAt(0);
//
//            DataFormatter formatter = new DataFormatter();
//
//            for (int rowNum = 1;
//                 rowNum <= sheet.getLastRowNum();
//                 rowNum++) {
//
//                Row row = sheet.getRow(rowNum);
//
//                if (isRowEmpty(row)) {
//                    continue;
//                }
//
//                String carName =
//                        getStringValue(formatter, row, 2);
//
//                if (carName.isBlank()) {
//                    continue;
//                }
//
//                //Dealer ID
//                Long dealerId =
//                        getLongValue(formatter, row, 0);
//
//                Dealer dealer =
//                        dealerRepository.findById(dealerId)
//                                .orElseThrow(() ->
//                                        new RuntimeException(
//                                                "Dealer not found : "
//                                                        + dealerId));
//
//                Vehicle car = new Vehicle();
//
//                car.setDealer(dealer);
//
//                // Location
//                car.setCity(
//                        getStringValue(formatter, row, 1));
//
//                // Car Name
//                car.setModel(
//                        getStringValue(formatter, row, 2));
//
//                // Variant
//                car.setVariant(
//                        getStringValue(formatter, row, 3));
//
//                // Brand
//                car.setBrand(
//                        getStringValue(formatter, row, 4));
//
//                // Description
//                car.setVehicleDescription(
//                        getStringValue(formatter, row, 5));
//
//                // Fuel Type
//                String fuelType =
//                        getStringValue(formatter, row, 6);
//
//                if (fuelType.length() > 100) {
//                    fuelType =
//                            fuelType.substring(0, 100);
//                }
//
//                car.setFuelType(fuelType);
//
//                // KM Driven
//                car.setKilometerDriven(
//                        getLongValue(formatter, row, 7));
//
//                // Price
//                car.setAskingPrice(
//                        getDoubleValue(formatter, row, 8));
//
//                // Owners
//                car.setOwnershipDetails(
//                        getLongValue(formatter, row, 9)
//                                .intValue());
//
//                // Model Year
//                car.setRegistrationYear(
//                        getLongValue(formatter, row, 10)
//                                .intValue());
//
//                // Dealer Contact Name
//                car.setDealerContactName(
//                        getStringValue(formatter, row, 29));
//
//                // Dealer Contact Number
//                car.setDealerContactNumber(
//                        getStringValue(formatter, row, 30));
//
//                //Finance Availability
//                car.setFinanceAvailability(false);
//
//                //Vehicle Type
//                car.setVehicleType(
//                        VehicleType.NON_PREMIUM);
//
//                //Vehicle Status
//                car.setVehicleStatus(
//                        VehicleStatus.ACTIVE);
//
//                //Create At
//                car.setCreatedAt(
//                        LocalDateTime.now());
//
//                // Images
//                int imageCount = 0;
//
//                for (int col = 11;
//                     col <= 28;
//                     col++) {
//
//                    String imageName =
//                            "row_" + (rowNum + 5)
//                                    + "_col_" + col
//                                    + ".jpg";
//
//                    File imageFile =
//                            new File(
//                                    uploadPath
//                                            + "/Images_Processed/"
//                                            + imageName);
//
//                    if (imageFile.exists()) {
//
//                        VehicleMedia image =
//                                new VehicleMedia();
//
//                        image.setFileName(
//                                imageName);
//
//                        image.setFileType(
//                                "jpg");
//
//                        image.setFilePath(
//                                "/uploads/olx/Images_Processed/"
//                                        + imageName);
//
//                        image.setMediaType(
//                                "IMAGE");
//
//                        image.setUploadedAt(
//                                LocalDateTime.now());
//
//                        image.setVehicle(car);
//
//                        car.getMediaList()
//                                .add(image);
//
//                        imageCount++;
//                    }
//                }
//
//                vehicleRepository.save(car);
//
//                System.out.println(
//                        "Imported Row : "
//                                + rowNum
//                                + " | Vehicle : "
//                                + car.getBrand()
//                                + " "
//                                + car.getModel()
//                                + " | Images : "
//                                + imageCount);
//            }
//        }
//    }
//


//    @Override
//    public void importData(MultipartFile excel,
//                           MultipartFile zip) throws Exception {
//
//        String uploadPath = "uploads/olx";
//
//        zipExtractor.unzip(zip, uploadPath);
//
//        try (Workbook workbook =
//                     WorkbookFactory.create(excel.getInputStream())) {
//
//            Sheet sheet = workbook.getSheetAt(0);
//
//            DataFormatter formatter = new DataFormatter();
//
//            for (int rowNum = 1;
//                 rowNum <= sheet.getLastRowNum();
//                 rowNum++) {
//
//                Row row = sheet.getRow(rowNum);
//
//                if (isRowEmpty(row)) {
//                    continue;
//                }
//
//                String carName =
//                        getStringValue(formatter, row, 1);
//
//                if (carName.isBlank()) {
//                    continue;
//                }
//
//                Vehicle car = new Vehicle();
//
//                Long dealerId = getLongValue(formatter, row, 0);
//
//                Dealer dealer = dealerRepository.findById(dealerId)
//                        .orElseThrow(() ->
//                                new RuntimeException(
//                                        "Dealer not found with id : " + dealerId));
//
//                car.setDealer(dealer);
//
//                car.setCity(
//                        getStringValue(formatter, row, 1));
//
//                car.setModel(
//                        getStringValue(formatter, row, 2));
//
//                car.setVariant(
//                        getStringValue(formatter, row, 3));
//
//                car.setBrand(
//                        getStringValue(formatter, row, 4));
//
//                car.setVehicleDescription(
//                        getStringValue(formatter, row, 5));
//
//                String fuelType =
//                        getStringValue(formatter, row, 6);
//
//                if (fuelType.length() > 100) {
//                    fuelType = fuelType.substring(0, 100);
//                }
//
//                car.setFuelType(fuelType);
//
//                car.setKilometerDriven(
//                        getLongValue(formatter, row, 7));
//
//                car.setAskingPrice(
//                        getDoubleValue(formatter, row, 8));
//
//                car.setOwnershipDetails(
//                        getLongValue(formatter, row, 9).intValue());
//
//                car.setRegistrationYear(
//                        getLongValue(formatter, row, 10).intValue());
//
//                car.setSubLocalityId(
//                        getLongValue(formatter, row, 12));
//
//                car.setDealerContactName(
//                        getStringValue(formatter, row, 32));
//
//                car.setDealerContactNumber(
//                        getStringValue(formatter, row, 33));
//
//                car.setFinanceAvailability(false);
//
//                car.setCreatedAt(LocalDateTime.now());
//
//                car.setVehicleType(VehicleType.NON_PREMIUM);
//
//                car.setVehicleStatus(VehicleStatus.ACTIVE);
//
//                int imageCount = 0;
//
//                for (int col = 13; col <= 30; col++) {
//
//                    String imageName =
//                            "row_" + (rowNum + 1) +
//                                    "_col_" + col + ".jpg";
//
//                    File imageFile =
//                            new File(uploadPath
//                                    + "/Images_Processed/"
//                                    + imageName);
//
//                    if (imageFile.exists()) {
//
//                        VehicleMedia image =
//                                new VehicleMedia();
//
//                        System.out.println("Checking : " + imageFile.getAbsolutePath());
//                        System.out.println("Exists : " + imageFile.exists());
//
//                        image.setFileName(imageName);
//                        image.setFileType("jpg"); // किंवा extension काढून टाक
//                        image.setFilePath("/uploads/olx/Images_Processed/" + imageName);
//                        image.setMediaType("IMAGE");
//                        image.setUploadedAt(LocalDateTime.now());
//
//                        image.setVehicle(car);
//
//                        car.getMediaList().add(image);
//
//                        imageCount++;
//
//                    }
//                }
//
//                vehicleRepository.save(car);
//
//                System.out.println(
//                        "Imported Row : "
//                                + rowNum
//                                + " | Car : "
//                                + carName
//                                + " | Images : "
//                                + imageCount);
//            }
//        }
//    }

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
                                    serverUrl +
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
                                                serverUrl +
                                                        image.getFilePath().replace("\\", "/")
                                        );
                                        return dto;

                                    }).toList();

                    response.setImages(images);

                    return response;

                }).toList();
    }
}