package com.autohub.configuration;

import java.util.List;


import com.autohub.entity.Admin;

import com.autohub.entity.Customer;
import com.autohub.entity.CustomerLead;
import com.autohub.entity.Dealer;
import com.autohub.repository.AdminRepository;

import com.autohub.repository.CustomerLeadRepository;
import com.autohub.repository.CustomerRepository;
import com.autohub.repository.DealerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {


    private final AdminRepository adminRepository;

    private final DealerRepository dealerRepository;

    private final CustomerRepository customerRepository;

    @Override
    public CustomUserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        boolean isEmail = username.contains("@");

        // ================= ADMIN =================

        Admin admin = isEmail
                ? adminRepository.findByEmail(username).orElse(null)
                : adminRepository.findByMobile(username).orElse(null);

        if (admin != null) {

            String role = admin.getRole() != null
                    ? admin.getRole().name()
                    : "ADMIN";

            return new CustomUserDetails(
                    admin.getAdminId(),
                    admin.getFullName(),
                    admin.getEmail(),
                    admin.getMobile(),
                    admin.getPassword(),
                    role,
                    List.of(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    )
            );
        }

        // ================= CUSTOMER =================

        Customer customer = isEmail
                ? customerRepository.findByEmail(username).orElse(null)
                : customerRepository.findByMobile(username).orElse(null);

        if (customer != null) {

            String role = customer.getRole() != null
                    ? customer.getRole().name()
                    : "CUSTOMER";

            return new CustomUserDetails(
                    customer.getId(),
                    customer.getCustomerName(),
                    customer.getEmail(),
                    customer.getMobile(),
                    customer.getPassword(),
                    role,
                    List.of(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    )
            );
        }

        // ================= DEALER =================

        Dealer dealer = null;

        if (isEmail) {

            dealer = dealerRepository
                    .findByEmail(username)
                    .orElse(null);

        } else {

            // Dealer Mobile
            dealer = dealerRepository
                    .findByDealerMobile(username)
                    .orElse(null);

            // Executive Mobile
            if (dealer == null) {

                dealer = dealerRepository
                        .findByExecutiveMobile(username)
                        .orElse(null);
            }
        }

        if (dealer != null) {

            String role = dealer.getRole() != null
                    ? dealer.getRole().name()
                    : "DEALER";

            return new CustomUserDetails(
                    dealer.getId(),
                    dealer.getOwnerName(),
                    dealer.getEmail(),
                    dealer.getDealerMobile(),
                    dealer.getPassword(),
                    role,
                    List.of(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    )
            );
        }

        throw new UsernameNotFoundException(
                "User not found with username: " + username
        );
    }
//    @Override
//    public CustomUserDetails loadUserByUsername(String mobile)
//            throws UsernameNotFoundException {
//
//
//        // ADMIN
////        Admin admin = adminRepository.findByEmail(email).orElse(null);
//        Admin admin = adminRepository.findByMobile(mobile).orElse(null);
//
//        if (admin != null) {
//
//            String role = admin.getRole() != null
//                    ? admin.getRole().name()
//                    : "ADMIN";
//
//            return new CustomUserDetails(
//                    admin.getAdminId(),
//                    admin.getFullName(),
//                    admin.getEmail(),
//                    admin.getMobile(),
//                    admin.getPassword(),
//                    role,
//                    List.of(
//                            new SimpleGrantedAuthority("ROLE_" + role)
//                    )
//            );
//        }
//
//        //Customer
//
////        Customer customer =
////                customerRepository.findByEmail(email)
////                        .orElse(null);
//
//        Customer customer =
//                customerRepository.findByMobile(mobile)
//                        .orElse(null);
//        if (customer != null) {
//
//            String role = customer.getRole() != null
//                    ? customer.getRole().name()
//                    : "CUSTOMER";
//
//            return new CustomUserDetails(
//                    customer.getId(),
//                    customer.getCustomerName(),
//                    customer.getEmail(),
//                    customer.getMobile(),
//                    customer.getPassword(),
//                    role,
//                    List.of(
//                            new SimpleGrantedAuthority("ROLE_" + role)
//                    )
//            );
//        }
//
//
//
//
////        // DEALER
////        Dealer dealer = dealerRepository.findByEmail(email).orElse(null);
//
//        Dealer dealer = dealerRepository.findByDealerMobile(mobile).orElse(null);
//
//        if (dealer != null) {
//
//            String role = dealer.getRole() != null
//                    ? dealer.getRole().name()
//                    : "DEALER";
//
//            return new CustomUserDetails(
//                    dealer.getId(),
//                    dealer.getOwnerName(),
//                    dealer.getEmail(),
//                    dealer.getDealerMobile(),
//                    dealer.getPassword(),
//                    role,
//                    List.of(
//                            new SimpleGrantedAuthority("ROLE_" + role)
//                    )
//            );
//        }
//
//        throw new UsernameNotFoundException(
//                "User not found with mobile: " + mobile
//        );
//    }
}