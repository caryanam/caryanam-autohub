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

import org.apache.poi.ss.usermodel.CellType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.Locale;

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

        // ------------------------------------------------------------------
        // STEP 0: Clear any leftover images from a PREVIOUS import run.
        // ------------------------------------------------------------------
        File imageDir = new File(uploadPath + "/Images_Processed");

        if (imageDir.exists()) {
            FileSystemUtils.deleteRecursively(imageDir);
            System.out.println("Cleared stale image directory before extracting new zip.");
        }

        zipExtractor.unzip(zip, uploadPath);

        // ------------------------------------------------------------------
        // STEP 1: Index every extracted image by its FILENAME (recursive, in
        // case the zip has nested folders).
        // ------------------------------------------------------------------
        Map<String, File> imagesByFileName = new HashMap<>();

        System.out.println("Image Dir Exists : " + imageDir.exists());
        System.out.println("Image Dir Path   : " + imageDir.getAbsolutePath());

        if (imageDir.exists() && imageDir.isDirectory()) {

            List<File> allImages = new ArrayList<>();

            try (Stream<Path> paths = Files.walk(imageDir.toPath())) {
                paths.filter(Files::isRegularFile)
                        .forEach(p -> allImages.add(p.toFile()));
            }

            System.out.println("Total Files Found : " + allImages.size());

            for (File file : allImages) {
                imagesByFileName.put(
                        file.getName().toLowerCase(Locale.ROOT),
                        file);
            }
        }

        System.out.println("Total Images Indexed : " + imagesByFileName.size());

        // Excel columns image1..image18 -> 0-indexed cell columns 11..28
        final int FIRST_IMAGE_COL = 11;
        final int LAST_IMAGE_COL = 28;

        int rowsProcessed = 0;
        int rowsSucceeded = 0;
        int totalImagesSaved = 0;
        int imagesReferencedButMissing = 0;
        List<String> failures = new ArrayList<>();
        List<String> missingImageWarnings = new ArrayList<>();

        // ------------------------------------------------------------------
        // STEP 2: Walk the Excel rows; resolve each row's images by filename.
        // ------------------------------------------------------------------
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

                    // TODO: replace 31 with the actual column index once confirmed
                    car.setVehicleType(resolveVehicleType(getStringValue(formatter, row, 31)));

                    car.setFinanceAvailability(false);
                   // car.setVehicleType(VehicleType.NON_PREMIUM);
                    car.setVehicleStatus(VehicleStatus.ACTIVE);
                    car.setCreatedAt(LocalDateTime.now());

                    int imageCount = 0;

                    for (int col = FIRST_IMAGE_COL; col <= LAST_IMAGE_COL; col++) {

                        String rawCellText = getImageCellRawText(row, col, formatter);
                        String fileName = extractImageFileName(rawCellText);

                        if (fileName == null || fileName.isBlank()) {
                            continue;
                        }

                        File imageFile = imagesByFileName.get(fileName.toLowerCase(Locale.ROOT));

                        if (imageFile == null) {
                            String warn = "Row " + rowNum
                                    + ": Excel references image '" + fileName
                                    + "' but it was not found in the extracted zip.";
                            System.out.println("WARNING: " + warn);
                            missingImageWarnings.add(warn);
                            imagesReferencedButMissing++;
                            continue;
                        }

                        String extension =
                                fileName.substring(fileName.lastIndexOf(".") + 1);

                        VehicleMedia image = new VehicleMedia();

                        image.setFileName(fileName);
                        image.setFileType(extension);
                        image.setFilePath(
                                "/uploads/olx/Images_Processed/" + fileName);
                        image.setMediaType("IMAGE");
                        image.setUploadedAt(LocalDateTime.now());
                        image.setVehicle(car);

                        car.getMediaList().add(image);

                        imageCount++;
                    }

                    vehicleRepository.save(car);

                    rowsSucceeded++;
                    totalImagesSaved += imageCount;

                    System.out.println(
                            "Imported Row : "
                                    + rowNum
                                    + " | Vehicle : "
                                    + car.getBrand()
                                    + " "
                                    + car.getModel()
                                    + " | Images : "
                                    + imageCount);

                } catch (Exception rowEx) {

                    String msg = "Row " + rowNum + " FAILED: " + rowEx.getMessage();

                    System.out.println("ERROR: " + msg);
                    failures.add(msg);
                }
            }
        }

        // ------------------------------------------------------------------
        // STEP 3: End-of-run summary.
        // ------------------------------------------------------------------
        System.out.println("==================== IMPORT SUMMARY ====================");
        System.out.println("Excel data rows processed        : " + rowsProcessed);
        System.out.println("Vehicles saved                    : " + rowsSucceeded);
        System.out.println("Vehicles failed                    : " + failures.size());
        System.out.println("Images indexed on disk             : " + imagesByFileName.size());
        System.out.println("Images saved to DB                 : " + totalImagesSaved);
        System.out.println("Images referenced but missing on disk : " + imagesReferencedButMissing);

        if (!failures.isEmpty()) {
            System.out.println("---- Failed rows ----");
            failures.forEach(System.out::println);
        }

        if (!missingImageWarnings.isEmpty()) {
            System.out.println("---- Missing image references ----");
            missingImageWarnings.forEach(System.out::println);
        }
        System.out.println("=========================================================");
    }

    /**
     * Excel image cells are often HYPERLINK formulas, e.g.:
     *   =HYPERLINK("Excel_Processed_Images1/row_1491_col_22.jpg","Processed Image")
     * DataFormatter does not reliably evaluate these into a clean path - for a
     * FORMULA cell we grab the raw formula text directly instead, so we can
     * parse the actual path argument ourselves.
     */
    private String getImageCellRawText(Row row, int colIndex, DataFormatter formatter) {

        Cell cell = row.getCell(colIndex);

        if (cell == null) {
            return null;
        }

        if (cell.getCellType() == CellType.FORMULA) {
            return cell.getCellFormula();
        }

        return formatter.formatCellValue(cell);
    }

    private static final Pattern HYPERLINK_ARG_PATTERN = Pattern.compile(
            "HYPERLINK\\(\\s*\"(.*?)\"\\s*,", Pattern.CASE_INSENSITIVE);

    /**
     * Pulls the actual filename out of a raw cell value, which may be:
     *   - a plain path: "Excel_Processed_Images1/row_6_col_13.jpg"
     *   - a HYPERLINK formula: HYPERLINK("Excel_Processed_Images1/row_6_col_13.jpg","Processed Image")
     * Returns just "row_6_col_13.jpg" in either case, or null if nothing usable.
     */
    private String extractImageFileName(String rawCellValue) {

        if (rawCellValue == null) {
            return null;
        }

        String value = rawCellValue.trim();

        if (value.isEmpty()) {
            return null;
        }

        Matcher hyperlinkMatcher = HYPERLINK_ARG_PATTERN.matcher(value);

        if (hyperlinkMatcher.find()) {
            value = hyperlinkMatcher.group(1);
        }

        value = value.replace('\\', '/');

        // Defensive: if any stray quote/formula debris survived, cut it off.
        if (value.contains("\"")) {
            value = value.substring(0, value.indexOf('"'));
        }

        String fileName = value.contains("/")
                ? value.substring(value.lastIndexOf('/') + 1)
                : value;

        // Final catch-all: strip any stray quotes, parentheses, or whitespace
        // left over from formula variants we didn't explicitly parse above
        // (e.g. escaped "" quoting, unbalanced quotes from a bad CSV->Excel
        // conversion, etc). This guarantees the filename we hand back never
        // has leftover formula punctuation attached, regardless of which
        // malformed pattern produced it.
        fileName = fileName
                .replaceAll("^[\"'()\\s]+", "")
                .replaceAll("[\"'()\\s]+$", "");

        return fileName.trim();
    }
    /**
     * Converts a free-text cell value like "premium", "Non Premium", "NON-PREMIUM"
     * into the VehicleType enum. Falls back to NON_PREMIUM (and logs a warning)
     * for blank or unrecognized values, so one bad cell doesn't fail the row.
     */
    private VehicleType resolveVehicleType(String rawValue) {

        if (rawValue == null || rawValue.isBlank()) {
            return VehicleType.NON_PREMIUM;
        }

        String normalized = rawValue.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');

        try {
            return VehicleType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            System.out.println(
                    "WARNING: Unrecognized vehicle type '" + rawValue
                            + "', defaulting to NON_PREMIUM");
            return VehicleType.NON_PREMIUM;
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