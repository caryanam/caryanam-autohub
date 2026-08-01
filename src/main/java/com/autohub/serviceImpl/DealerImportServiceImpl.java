package com.autohub.serviceImpl;

import com.autohub.entity.Dealer;
import com.autohub.enums.DealerStatus;
import com.autohub.enums.Role;
import com.autohub.enums.SubscriptionPlan;
import com.autohub.repository.DealerRepository;
import com.autohub.service.DealerImportService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class DealerImportServiceImpl
        implements DealerImportService {

    private final DealerRepository dealerRepository;

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void importDealerData(MultipartFile excel) throws Exception {

        try (Workbook workbook = WorkbookFactory.create(excel.getInputStream())) {

            Sheet sheet = workbook.getSheetAt(0);

            DataFormatter formatter = new DataFormatter();

            int success = 0;
            int failed = 0;

            java.util.Set<String> processedMobiles = new java.util.HashSet<>();
            java.util.Set<String> processedWhatsapps = new java.util.HashSet<>();
            java.util.Set<String> processedEmails = new java.util.HashSet<>();

            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {

                try {

                    Row row = sheet.getRow(rowNum);

                    if (row == null) {
                        continue;
                    }

                    Dealer dealer = new Dealer();

                    String dealerName = getStringValue(formatter, row, 1);

                    dealer.setOwnerName(
                            dealerName.isBlank()
                                    ? "Unknown Dealer " + rowNum
                                    : dealerName);

                    dealer.setBusinessName(
                            dealerName.isBlank()
                                    ? "Unknown Dealer " + rowNum
                                    : dealerName);

                    Long years = getLongValue(formatter, row, 2);

                    dealer.setYearsInBusiness(
                            years != null
                                    ? years.intValue()
                                    : 0);

                    String mobile = getStringValue(formatter, row, 3);
                    if (mobile.isBlank()) {
                        mobile = "NA_" + rowNum;
                    } else {
                        if (dealerRepository.existsByDealerMobile(mobile)) {
                            throw new RuntimeException("Mobile already registered in system: " + mobile);
                        }
                        if (!processedMobiles.add(mobile)) {
                            throw new RuntimeException("Duplicate mobile found in excel: " + mobile);
                        }
                    }
                    dealer.setDealerMobile(mobile);

                    String whatsapp = getStringValue(formatter, row, 4);
                    if (whatsapp.isBlank()) {
                        whatsapp = dealer.getDealerMobile();
                    }
                    if (!whatsapp.startsWith("NA_")) {
                        if (dealerRepository.existsByWhatsapp(whatsapp)) {
                            throw new RuntimeException("WhatsApp number already registered in system: " + whatsapp);
                        }
                        if (!processedWhatsapps.add(whatsapp)) {
                            throw new RuntimeException("Duplicate WhatsApp number found in excel: " + whatsapp);
                        }
                    }
                    dealer.setWhatsapp(whatsapp);

                    String password = getStringValue(formatter, row, 5);

                    if (password.isBlank()) {
                        password = "pass@123";
                    }

                    dealer.setPassword(
                            passwordEncoder.encode(password));

                    dealer.setAddress(
                            getStringValue(formatter, row, 6).isBlank()
                                    ? "NA"
                                    : getStringValue(formatter, row, 6));

                    dealer.setCity(
                            getStringValue(formatter, row, 7).isBlank()
                                    ? "NA"
                                    : getStringValue(formatter, row, 7));

                    dealer.setState(
                            getStringValue(formatter, row, 8).isBlank()
                                    ? "NA"
                                    : getStringValue(formatter, row, 8));

                    dealer.setPinCode(
                            getStringValue(formatter, row, 9).isBlank()
                                    ? "000000"
                                    : getStringValue(formatter, row, 9));

                    String executiveNumber = getStringValue(formatter, row, 10);

                    dealer.setExecutiveMobile(
                            executiveNumber != null && !executiveNumber.trim().isEmpty()
                                    ? executiveNumber.trim()
                                    : null);

                    String email = getStringValue(formatter, row, 11);

                    if (email != null && !email.trim().isEmpty()) {
                        email = email.trim();
                        if (dealerRepository.existsByEmail(email)) {
                            throw new RuntimeException("Email already registered in system: " + email);
                        }
                        if (!processedEmails.add(email)) {
                            throw new RuntimeException("Duplicate email found in excel: " + email);
                        }
                        dealer.setEmail(email);
                    } else {
                        dealer.setEmail(null);
                    }
                    dealer.setRole(Role.DEALER);

                    dealer.setDealerAccountStatus(DealerStatus.APPROVED);

                    dealer.setFreeTrialEndDate(LocalDateTime.now().plusMonths(1));

                    dealer.setSubscriptionPlan(SubscriptionPlan.BASIC);
                    dealer.setSubscriptionStartDate(LocalDateTime.now());
                    dealer.setSubscriptionEndDate(LocalDateTime.now().plusMonths(1));

                    dealer.setSubscriptionActive(true);

                    Dealer save = dealerRepository.save(dealer);

                    System.out.println(
                            "Saved Dealer: " + save);

                    success++;

                    System.out.println(
                            "Saved Dealer Row : " + rowNum);

                    System.out.println(
                            "Last Row Number : "
                                    + sheet.getLastRowNum());

                } catch (Exception e) {

                    failed++;

                    System.out.println(
                            "Failed Row : "
                                    + rowNum
                                    + " Error : "
                                    + e.getMessage());

                    e.printStackTrace();

                }
            }

            System.out.println(
                    "Success : " + success);

            System.out.println(
                    "Failed : " + failed);
        }
    }

    private String getStringValue(DataFormatter formatter, Row row, int index) {

        Cell cell = row.getCell(index);

        if (cell == null) {
            return "";
        }

        return formatter.formatCellValue(cell).trim();
    }

    private Long getLongValue(DataFormatter formatter, Row row, int index) {

        try {

            String value = getStringValue(formatter, row, index);

            if (value.isBlank()) {
                return 0L;
            }

            return Long.parseLong(value.replace(".0", ""));

        } catch (Exception e) {

            return 0L;
        }
    }
}
