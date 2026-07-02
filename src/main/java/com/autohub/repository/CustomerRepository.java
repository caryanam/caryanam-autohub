package com.autohub.repository;

import com.autohub.entity.Customer;
import com.autohub.entity.Dealer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer,Long> {

    Optional<Customer> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobileNumber);

    Optional<Customer> findByMobile(String mobile);



}
