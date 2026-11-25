package com.printforge.repository;

import com.printforge.entity.Order;
import com.printforge.entity.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
  List<Order> findByCustomerOrderByCreatedAtDesc(User customer);
  Optional<Order> findByIdAndCustomer(Long id, User customer);

  /** Loads an order with a row-level write lock held until the transaction ends. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select o from Order o where o.id = :id")
  Optional<Order> findByIdForUpdate(@Param("id") Long id);
}
