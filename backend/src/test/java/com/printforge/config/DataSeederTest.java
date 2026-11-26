package com.printforge.config;

import com.printforge.entity.Product;
import com.printforge.entity.Role;
import com.printforge.entity.User;
import com.printforge.repository.CategoryRepository;
import com.printforge.repository.ProductRepository;
import com.printforge.repository.UserRepository;
import com.printforge.service.ProductImageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Demo accounts have published passwords, so they must never appear unless explicitly asked for. */
class DataSeederTest {
  private final UserRepository users = mock(UserRepository.class);
  private final CategoryRepository categories = mock(CategoryRepository.class);
  private final ProductRepository products = mock(ProductRepository.class);
  private final ProductImageService productImages = mock(ProductImageService.class);

  private DataSeeder seeder(boolean enabled) {
    DataSeeder seeder = new DataSeeder(users, categories, products, new BCryptPasswordEncoder(4), new TransactionTemplate(mock(PlatformTransactionManager.class)), productImages);
    ReflectionTestUtils.setField(seeder, "enabled", enabled);
    when(categories.count()).thenReturn(1L);
    when(products.count()).thenReturn(1L);
    return seeder;
  }

  @Test void seedingIsOffByDefault() throws Exception {
    Field enabled = DataSeeder.class.getDeclaredField("enabled");
    String expression = enabled.getAnnotation(org.springframework.beans.factory.annotation.Value.class).value();
    assertTrue(expression.endsWith(":false}"), "the code default must be off, was " + expression);
  }

  @Test void disabledSeederCreatesNothing() {
    seeder(false).afterSingletonsInstantiated();
    verifyNoInteractions(users, categories, products);
  }

  @Test void demoAccountsAreNotAddedToADatabaseThatHasUsers() {
    when(users.count()).thenReturn(3L);
    seeder(true).afterSingletonsInstantiated();
    verify(users, never()).save(any());
  }

  @Test void deletingTheDemoAdminDoesNotBringItBack() {
    // The old seeder re-created admin@printforge.com on every start whenever that email was missing.
    when(users.count()).thenReturn(1L);
    when(users.existsByEmailIgnoreCase("admin@printforge.com")).thenReturn(false);
    seeder(true).afterSingletonsInstantiated();
    verify(users, never()).save(any());
  }

  @Test void emptyDatabaseGetsBothDemoAccountsWhenEnabled() {
    when(users.count()).thenReturn(0L);
    seeder(true).afterSingletonsInstantiated();
    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(users, times(2)).save(saved.capture());
    List<User> created = saved.getAllValues();
    assertEquals(List.of("admin@printforge.com", "customer@printforge.com"), created.stream().map(User::getEmail).toList());
    assertEquals(List.of(Role.ADMIN, Role.CUSTOMER), created.stream().map(User::getRole).toList());
  }

  @Test void everyDemoProductGetsItsBundledPhoto() throws Exception {
    DataSeeder seeder = seeder(true);
    when(users.count()).thenReturn(1L);
    when(products.count()).thenReturn(0L);
    AtomicLong ids = new AtomicLong();
    when(products.save(any(Product.class))).thenAnswer(call -> {
      Product product = call.getArgument(0);
      ReflectionTestUtils.setField(product, "id", ids.incrementAndGet());
      return product;
    });

    seeder.afterSingletonsInstantiated();

    // A renamed product or a missing file under resources/seed/product-images would skip a photo.
    verify(products, times(15)).save(any(Product.class));
    verify(productImages, times(15)).storeBundled(anyLong(), any(InputStream.class), eq("jpg"));
  }

  @Test void slugsMatchThePhotoFileNames() {
    assertEquals("cable-clip-pack-x6", DataSeeder.slug("Cable Clip Pack (x6)"));
    assertEquals("desk-pen-card-tray", DataSeeder.slug("Desk Pen & Card Tray"));
    assertEquals("bookend-pair-geometric", DataSeeder.slug("Bookend Pair (Geometric)"));
  }
}
