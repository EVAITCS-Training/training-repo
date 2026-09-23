package com.evaitcs.securebank.account.repository;

import com.evaitcs.securebank.account.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
}
