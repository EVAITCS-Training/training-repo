package com.evaitcs.securebank.repository;

import com.evaitcs.securebank.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * REPOSITORY LAYER — "repository" package.
 *
 * THE BIG IDEA: you write an INTERFACE, Spring Data JPA generates the
 * implementation at startup. No JDBC boilerplate, no SQL strings for simple
 * cases. It works by PARSING METHOD NAMES ("derived queries"):
 *
 *   findByEmail(String email)
 *   └─┬──┘└─┬─┘
 *   keyword  property of Customer  ->  SELECT * FROM customer WHERE email = ?
 *
 * Extending JpaRepository gives you, for free:
 *   save(), findById(), findAll(), deleteById(), count(), ...
 * plus paging/sorting (Pageable/Page) and transaction helpers.
 *
 * CONNECTION: services depend on these interfaces (dependency inversion —
 * Week 4 OOP). In unit tests you can MOCK an interface with Mockito, which
 * is why services are trivially testable (see TransferServiceTest).
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /** Derived query -> SELECT * FROM customer WHERE email = ? */
    boolean existsByEmail(String email);
}
