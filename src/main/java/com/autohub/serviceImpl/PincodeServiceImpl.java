package com.autohub.serviceImpl;

import com.autohub.dto.AreaResponseDTO;
import com.autohub.entity.PincodeMaster;
import com.autohub.repository.PincodeMasterRepository;
import com.autohub.service.PincodeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PincodeServiceImpl
        implements PincodeService {

    private final PincodeMasterRepository repository;

    @Override
    @Transactional
    public void importExcel(MultipartFile file) throws Exception {

        List<PincodeMaster> records = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {

                Row row = sheet.getRow(rowIndex);

                if (row == null) {
                    continue;
                }

                String city = getCellValue(
                        row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK));

                String area = getCellValue(
                        row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK));

                String pincode = getCellValue(
                        row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK));

                String nearBy = getCellValue(
                        row.getCell(3, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK));

                // Completely empty row skip
                if (city.trim().isEmpty()
                        && area.trim().isEmpty()
                        && pincode.trim().isEmpty()
                        && nearBy.trim().isEmpty()) {
                    continue;
                }

                PincodeMaster entity = new PincodeMaster();
                entity.setCity(city.trim());
                entity.setArea(area.trim());
                entity.setPincode(pincode.trim());
                entity.setNearBy(nearBy.trim());

                records.add(entity);
            }
        }

        if (!records.isEmpty()) {
            repository.saveAll(records);
        }
    }

    @Override
    public List<String> getCities() {
        return repository.getAllCities();
    }

    @Override
    public List<String> getAreas(String city) {
        return repository.getAreasByCity(city);
    }

    @Override
    public List<AreaResponseDTO> getByArea(String area) {

        return repository.findAllByArea(area)
                .stream()
                .map(this::map)
                .toList();
    }

    @Override
    public List<AreaResponseDTO> getByPincode(String pincode) {

        List<PincodeMaster> data =
                repository.findAllByPincode(pincode);

        return data.stream()
                .map(this::map)
                .toList();
    }

    @Override
    public List<String> getAllAreas() {
        return repository.getAllAreas();
    }


    private AreaResponseDTO map(PincodeMaster p) {

        return new AreaResponseDTO(
                p.getCity(),
                p.getArea(),
                p.getPincode(),
                p.getNearBy()
        );
    }




    private String getCellValue(Cell cell) {

        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {

            case STRING ->
                    cell.getStringCellValue().trim();

            case NUMERIC -> {

                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }

                yield String.valueOf(
                        (long) cell.getNumericCellValue());
            }

            case BOOLEAN ->
                    String.valueOf(cell.getBooleanCellValue());

            case FORMULA ->
                    cell.getCellFormula();

            default -> "";
        };
    }
}
