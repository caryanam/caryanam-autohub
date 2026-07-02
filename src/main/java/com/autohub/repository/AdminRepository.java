package com.autohub.repository;




import com.autohub.entity.Admin;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByMobile(String email);

    Optional<Admin> findByMobile(String mobile);


}

