package com.autohub.controller;

import com.autohub.dto.CustomerWishlistDTO;
import com.autohub.dto.DealerWishlistDTO;
import com.autohub.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    //FOR CUSTOMER
    @PostMapping("/add-wishlist/{customerId}/{vehicleId}")
    public ResponseEntity<String> addToWishlist(@PathVariable Long customerId,@PathVariable Long vehicleId) {
        return new ResponseEntity<>(wishlistService.addToWishlist(customerId, vehicleId), HttpStatus.OK);
    }

    //FOR CUSTOMER
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<CustomerWishlistDTO>> getCustomerWishlist(@PathVariable Long customerId) {

        return new ResponseEntity<>(wishlistService.getCustomerWishlist(customerId), HttpStatus.OK);
    }


    //FOR DEALER
    @GetMapping("/dealer/{dealerId}")
    public ResponseEntity<List<DealerWishlistDTO>>
    getDealerWishlist(@PathVariable Long dealerId) {

        return ResponseEntity.ok(
                wishlistService.getDealerWishlist(dealerId)
        );
    }

    //FOR CUSTOMER
    @DeleteMapping("/customer/remove/{customerId}/{vehicleId}")
    public ResponseEntity<String> removeWishlist( @PathVariable Long customerId,  @PathVariable Long vehicleId) {

        return new ResponseEntity<>(wishlistService.removeWishlist(customerId, vehicleId), HttpStatus.OK);
    }
}
