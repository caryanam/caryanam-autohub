package com.autohub.service;

import org.springframework.web.multipart.MultipartFile;

public interface DealerImportService {

    void importDealerData(
            MultipartFile excel)
            throws Exception;
}
