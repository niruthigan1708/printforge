package com.printforge.repository;

import com.printforge.entity.Category;
import com.printforge.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
  List<Product> findByActiveTrueOrderByCreatedAtDesc();
  List<Product> findAllByOrderByCreatedAtDesc();
  List<Product> findByActiveTrueAndNameContainingIgnoreCaseOrderByCreatedAtDesc(String search);
  List<Product> findByActiveTrueAndCategory_NameIgnoreCaseOrderByCreatedAtDesc(String category);
  boolean existsByCategory(Category category);
}
