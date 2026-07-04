package com.autohub.repository;

import com.autohub.entity.Customer;
import com.autohub.entity.CustomerLead;
import com.autohub.entity.Dealer;
import com.autohub.enums.CustomerLeadStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerLeadRepository extends JpaRepository<CustomerLead,Long> {

    // Dealer wise leads find
    List<CustomerLead> findByDealerId(Long dealerId);

    // Vehicle delete leads also delete

    @Modifying
    @Transactional
    @Query("DELETE FROM CustomerLead l WHERE l.vehicle.id = :vehicleId")
    void deleteLeadsByVehicleId(@Param("vehicleId") Long vehicleId);


    // Month wise Customer Leads Analytics
    @Query("""
    SELECT MONTH(cl.enquiryDate),
           COUNT(cl),
           SUM(CASE WHEN cl.leadStatus = 'CONVERTED' THEN 1 ELSE 0 END)
    FROM CustomerLead cl
    WHERE cl.dealer.id = :dealerId
    AND YEAR(cl.enquiryDate) = YEAR(CURRENT_DATE)
    GROUP BY MONTH(cl.enquiryDate)
    ORDER BY MONTH(cl.enquiryDate)
""")
    List<Object[]> getMonthlyLeadAnalytics(@Param("dealerId") Long dealerId);

    long countByDealerIdAndLeadStatus(Long dealerId,CustomerLeadStatus leadStatus);


    // Total Leads
    long countByDealerId(Long dealerId);

    //Count of leads
    long count();

    //Admin dashboard
    @Query("""
    SELECT MONTH(cl.enquiryDate),
           COUNT(cl)
    FROM CustomerLead cl
    GROUP BY MONTH(cl.enquiryDate)
    ORDER BY MONTH(cl.enquiryDate)
""")
    List<Object[]> getMonthlyLeadAnalytics();


    // Dealer can see only lead created customer

    @Query("""
SELECT DISTINCT c
FROM CustomerLead cl
JOIN cl.customer c
WHERE cl.dealer.id = :dealerId
AND c IS NOT NULL
""")
    List<Customer> findCustomersByDealer(
            @Param("dealerId") Long dealerId
    );

    // Customer can see only dealer

    @Query("""
SELECT DISTINCT d
FROM CustomerLead cl
JOIN cl.dealer d
WHERE cl.customer.id = :customerId
""")
    List<Dealer> findDealersByCustomer(
            @Param("customerId") Long customerId
    );

    //boolean existsByDealerIdAndCustomerId(Long dealerId,Long customerId);
}
