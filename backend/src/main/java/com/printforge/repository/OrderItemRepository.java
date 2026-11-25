package com.printforge.repository;

import com.printforge.entity.OrderItem;
import com.printforge.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
  boolean existsByProduct(Product product);
}
