package com.autohub.service;

import com.autohub.dto.CustomerWishlistDTO;
import com.autohub.dto.DealerWishlistDTO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface WishlistService {

    String addToWishlist(Long customerId, Long vehicleId);

    List<CustomerWishlistDTO> getCustomerWishlist(Long customerId);

    List<DealerWishlistDTO> getDealerWishlist(Long dealerId);

    String removeWishlist(Long customerId, Long vehicleId);
}
