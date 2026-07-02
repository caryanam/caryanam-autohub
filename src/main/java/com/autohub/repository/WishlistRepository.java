package com.autohub.repository;

import com.autohub.entity.Customer;
import com.autohub.entity.Vehicle;
import com.autohub.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    boolean existsByCustomerAndVehicle(Customer customer, Vehicle vehicle);

    List<Wishlist> findByCustomer(Customer customer);

    @Query("""
    SELECT w
    FROM Wishlist w
    WHERE w.vehicle.dealer.id = :dealerId
""")
    List<Wishlist> findDealerWishlist(
            @Param("dealerId") Long dealerId
    );

   boolean existsByCustomer_IdAndVehicle_Id(
            Long customerId,
            Long vehicleId
    );
}