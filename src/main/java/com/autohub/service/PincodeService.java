package com.autohub.service;

import com.autohub.dto.AreaResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PincodeService {

    void importExcel(MultipartFile file) throws Exception;

    List<String> getCities();

    List<String> getAreas(String city);

    public List<AreaResponseDTO> getByArea(String area);

    public List<AreaResponseDTO> getByPincode(String pincode);

    public List<String> getAllAreas();
}
