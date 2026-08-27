package com.printforge.repository;

import com.printforge.entity.Order;
import com.printforge.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
  List<Order> findByCustomerOrderByCreatedAtDesc(User customer);
  Optional<Order> findByIdAndCustomer(Long id, User customer);
}
