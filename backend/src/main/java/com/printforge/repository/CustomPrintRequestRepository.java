package com.printforge.repository;

import com.printforge.entity.CustomPrintRequest;
import com.printforge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CustomPrintRequestRepository extends JpaRepository<CustomPrintRequest, Long> {
  List<CustomPrintRequest> findByCustomerOrderByCreatedAtDesc(User customer);
  Optional<CustomPrintRequest> findByIdAndCustomer(Long id, User customer);
  List<CustomPrintRequest> findAllByOrderByCreatedAtDesc();
}
