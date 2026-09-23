package com.evaitcs.securebank.payment.repository;

import com.evaitcs.securebank.payment.domain.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
}
