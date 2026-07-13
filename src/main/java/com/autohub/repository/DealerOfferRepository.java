package com.autohub.repository;

import com.autohub.entity.Dealer;
import com.autohub.entity.DealerOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DealerOfferRepository extends JpaRepository<DealerOffer, Long> {
    List<DealerOffer> findAllByOrderByCreatedAtDesc();


}
