package com.printforge;

import com.printforge.dto.CreateOrderRequest;
import com.printforge.dto.OrderResponse;
import com.printforge.entity.Category;
import com.printforge.entity.Product;
import com.printforge.entity.Role;
import com.printforge.repository.CategoryRepository;
import com.printforge.repository.ProductRepository;
import com.printforge.service.OrderService;
import com.printforge.support.TestTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/** Orders placed at the same moment must neither oversell stock nor share an order number. */
@SpringBootTest
class OrderConcurrencyTest {
  private static final int THREADS = 8;

  @Autowired OrderService orders;
  @Autowired ProductRepository products;
  @Autowired CategoryRepository categories;
  @Autowired TestTokens tokens;

  private Product newProduct(String name, int stock) {
    Category category = categories.save(new Category(name + " Category", "desc"));
    Product product = new Product();
    product.setName(name);
    product.setDescription("desc");
    product.setPrice(new BigDecimal("100.00"));
    product.setStockQuantity(stock);
    product.setMaterial("PLA");
    product.setColor("Black");
    product.setCategory(category);
    return products.save(product);
  }

  private CreateOrderRequest orderFor(Long productId, int quantity) {
    return new CreateOrderRequest(List.of(new CreateOrderRequest.Item(productId, quantity)), "Jane", "0771234567", "1 Road", "Colombo", "00300");
  }

  /** Fires all calls at once from separate threads; returns successes and the 409 rejections. */
  private List<Object> placeConcurrently(Long productId, int quantity) throws Exception {
    tokens.tokenFor("concurrent-customer@example.com", Role.CUSTOMER);
    ExecutorService pool = Executors.newFixedThreadPool(THREADS);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<Object>> futures = new ArrayList<>();
    for (int i = 0; i < THREADS; i++) {
      futures.add(pool.submit(() -> {
        start.await();
        try {
          return orders.create("concurrent-customer@example.com", orderFor(productId, quantity));
        } catch (ResponseStatusException rejected) {
          return rejected;
        }
      }));
    }
    start.countDown();
    List<Object> results = new ArrayList<>();
    for (Future<Object> future : futures) results.add(future.get(30, TimeUnit.SECONDS));
    pool.shutdown();
    return results;
  }

  @Test void concurrentOrdersNeverOversellStock() throws Exception {
    Product product = newProduct("Last Few Units", 3);

    List<Object> results = placeConcurrently(product.getId(), 1);

    long succeeded = results.stream().filter(OrderResponse.class::isInstance).count();
    long rejected = results.stream().filter(r -> r instanceof ResponseStatusException e && e.getStatusCode().value() == 409).count();
    assertEquals(3, succeeded, "exactly the available stock should be sold");
    assertEquals(THREADS - 3, rejected, "every other order should be rejected as out of stock");
    assertEquals(0, products.findById(product.getId()).orElseThrow().getStockQuantity());
  }

  @Test void concurrentOrdersGetUniqueOrderNumbers() throws Exception {
    Product product = newProduct("Plenty In Stock", 100);

    List<Object> results = placeConcurrently(product.getId(), 1);

    Set<String> numbers = new HashSet<>();
    for (Object result : results) {
      OrderResponse order = assertInstanceOf(OrderResponse.class, result, "every order should succeed");
      assertEquals("PF-" + (1000 + order.id()), order.orderNumber());
      numbers.add(order.orderNumber());
    }
    assertEquals(THREADS, numbers.size(), "no two orders may share a number");
  }

  @Test void repeatedLinesForOneProductAreMergedAndCheckedTogether() throws Exception {
    Product product = newProduct("Merged Lines", 3);
    tokens.tokenFor("merge-customer@example.com", Role.CUSTOMER);
    var twoLines = new CreateOrderRequest(List.of(new CreateOrderRequest.Item(product.getId(), 2), new CreateOrderRequest.Item(product.getId(), 2)), "Jane", "0771234567", "1 Road", "Colombo", "00300");

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> orders.create("merge-customer@example.com", twoLines));
    assertEquals(409, ex.getStatusCode().value());

    var fits = new CreateOrderRequest(List.of(new CreateOrderRequest.Item(product.getId(), 1), new CreateOrderRequest.Item(product.getId(), 2)), "Jane", "0771234567", "1 Road", "Colombo", "00300");
    OrderResponse order = orders.create("merge-customer@example.com", fits);
    assertEquals(1, order.items().size());
    assertEquals(3, order.items().get(0).quantity());
  }
}
