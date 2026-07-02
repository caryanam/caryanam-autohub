package com.autohub.repository;

import com.autohub.entity.Payment;
import com.autohub.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByDealerId(String dealerId);

    Optional<Payment> findTopByDealerIdAndPaymentStatusOrderByPaymentIdDesc(Long dealerId, PaymentStatus paymentStatus);

    Optional<Payment> findTopByDealerIdOrderByPaymentIdDesc(Long dealerId);

    @Query("""
            SELECT p.subscriptionPlan,
                   COUNT(p.paymentId),
                   SUM(p.amount)
            FROM Payment p
            WHERE p.paymentStatus='SUCCESS'
            GROUP BY p.subscriptionPlan
            """)
    List<Object[]> getRevenueByPlanReport();

    //Admin dashboard
    @Query("""
    SELECT MONTH(p.paymentDate),
           SUM(p.amount)
    FROM Payment p
    GROUP BY MONTH(p.paymentDate)
    ORDER BY MONTH(p.paymentDate)
""")
    List<Object[]> getMonthlyRevenueAnalytics();


    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p")
    Double getTotalRevenue();

}
