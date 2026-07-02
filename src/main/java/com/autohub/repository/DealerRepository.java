package com.autohub.repository;


import com.autohub.entity.Dealer;
import com.autohub.enums.DealerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DealerRepository extends JpaRepository<Dealer, Long> {

    Optional<Dealer> findByOwnerName(String ownerName);

    Optional<Dealer> findByDealerMobile(String mobile);

    Optional<Dealer> findByEmail(String email);

    Optional<Dealer> findByExecutiveMobile(String executiveMobile);

    boolean existsByExecutiveMobile(String executiveMobile);

    boolean existsByEmail(String email);

    boolean existsByDealerMobile(String mobileNumber);

    @Query("""
            SELECT d.city,
                   COUNT(d.id)
            FROM Dealer d
            GROUP BY d.city
            ORDER BY COUNT(d.id) DESC
            """)
    List<Object[]> getTopCitiesReport();

    long count();

    long countByDealerAccountStatus(DealerStatus dealerAccountStatus);

    //Admin dashboard
    @Query("""
    SELECT MONTH(d.createdAt),
           COUNT(d)
    FROM Dealer d
    GROUP BY MONTH(d.createdAt)
    ORDER BY MONTH(d.createdAt)
""")
    List<Object[]> getMonthlyDealerAnalytics();


}