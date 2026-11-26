package com.printforge.repository;

import com.printforge.entity.CustomPrintRequest;
import com.printforge.entity.RequestStatus;
import com.printforge.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CustomPrintRequestRepository extends JpaRepository<CustomPrintRequest, Long> {
  Page<CustomPrintRequest> findByCustomer(User customer, Pageable pageable);
  Optional<CustomPrintRequest> findByIdAndCustomer(Long id, User customer);
  List<CustomPrintRequest> findTop5ByOrderByCreatedAtDesc();

  /** Closed requests, untouched since {@code cutoff}, whose model file has not been purged yet. */
  List<CustomPrintRequest> findByStatusInAndUpdatedAtBeforeAndFilePurgedAtIsNull(Collection<RequestStatus> statuses, Instant cutoff);

  /**
   * Loads a request with a row-level write lock held until the transaction ends, so a customer's
   * answer and an admin's quote revision or status change cannot interleave.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select r from CustomPrintRequest r where r.id = :id")
  Optional<CustomPrintRequest> findByIdForUpdate(@Param("id") Long id);
}
