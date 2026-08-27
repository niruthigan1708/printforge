package com.printforge.config;

import com.printforge.entity.*;
import com.printforge.repository.CategoryRepository;
import com.printforge.repository.ProductRepository;
import com.printforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Seeds demo accounts, categories, and products on first boot so the MVP is immediately
 * demonstrable. The plaintext passwords below exist only to be BCrypt-hashed before storage
 * and are documented in the README as demo credentials — never real production secrets.
 */
@Component @RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
  private final UserRepository users;
  private final CategoryRepository categories;
  private final ProductRepository products;
  private final PasswordEncoder encoder;
  @Value("${app.seed.enabled:true}") private boolean enabled;

  @Override @Transactional
  public void run(String... args) {
    if (!enabled) return;
    seedUsers();
    Map<String, Category> categoryByName = seedCategories();
    seedProducts(categoryByName);
  }

  private void seedUsers() {
    if (!users.existsByEmailIgnoreCase("admin@printforge.com")) {
      User admin = new User();
      admin.setName("PrintForge Admin");
      admin.setEmail("admin@printforge.com");
      admin.setPassword(encoder.encode("Admin@12345"));
      admin.setRole(Role.ADMIN);
      users.save(admin);
    }
    if (!users.existsByEmailIgnoreCase("customer@printforge.com")) {
      User customer = new User();
      customer.setName("Demo Customer");
      customer.setEmail("customer@printforge.com");
      customer.setPassword(encoder.encode("Customer@12345"));
      customer.setRole(Role.CUSTOMER);
      users.save(customer);
    }
  }

  private Map<String, Category> seedCategories() {
    Map<String, Category> result = new LinkedHashMap<>();
    if (categories.count() == 0) {
      String[][] defs = {
        {"Desk Accessories", "Considered objects for a calmer desk."},
        {"Phone Accessories", "Stands, holders, and cable helpers for everyday devices."},
        {"Gaming", "Stands and organizers for consoles and controllers."},
        {"Home & Decor", "Small, sculptural pieces for the home."},
        {"Organization", "Modular storage for the things that pile up."},
        {"Miniatures", "Tabletop terrain and figures."},
        {"Educational", "Models and tools for learning and teaching."},
        {"Other", "Everything else we print."},
      };
      for (String[] def : defs) result.put(def[0], categories.save(new Category(def[0], def[1])));
    } else {
      categories.findAll().forEach(category -> result.put(category.getName(), category));
    }
    return result;
  }

  private void seedProducts(Map<String, Category> categoryByName) {
    if (products.count() > 0) return;
    Object[][] defs = {
      {"Adjustable Phone Stand", "Phone Accessories", "A tilt-adjustable stand that keeps your phone at the perfect viewing angle.", "1500", 40, "PLA", "Black"},
      {"Cable Management Box", "Desk Accessories", "Hide power strips and tangled cables behind a clean, vented enclosure.", "2200", 25, "PETG", "White"},
      {"Hexagon Desk Organizer", "Organization", "Modular hexagon trays that snap together to fit your desk layout.", "2650", 30, "PETG", "Grey"},
      {"Controller Stand", "Gaming", "A sturdy dual stand that keeps your controllers off the desk.", "1800", 35, "PLA", "Black"},
      {"Headphone Stand", "Desk Accessories", "A weighted stand with a clean silhouette for over-ear headphones.", "2200", 28, "PLA", "White"},
      {"Minimalist Plant Pot", "Home & Decor", "A geometric planter with a drainage tray, sized for small succulents.", "1450", 45, "PLA", "Terracotta"},
      {"Wall Key Holder", "Organization", "A five-hook rack that mounts in seconds with adhesive strips.", "1200", 50, "PLA", "Black"},
      {"Miniature Terrain Set", "Miniatures", "Four textured tiles for tabletop worlds, ready to prime and paint.", "4200", 15, "PLA", "Grey"},
      {"Cable Clip Pack (x6)", "Desk Accessories", "Six adhesive clips that route cables along any desk edge.", "950", 60, "PLA", "Black"},
      {"Monitor Riser", "Desk Accessories", "Raises your monitor to eye level and stores small items underneath.", "3200", 20, "PETG", "White"},
      {"Articulated Dragon Toy", "Miniatures", "A flexible, fully articulated print-in-place fidget dragon.", "1750", 32, "PLA", "Red"},
      {"Molecule Model Kit", "Educational", "A snap-together model kit for teaching basic molecular geometry.", "2800", 18, "PLA", "Blue"},
      {"Desk Pen & Card Tray", "Desk Accessories", "A shallow tray that keeps pens, cards, and small tools within reach.", "1350", 38, "PLA", "White"},
      {"Gaming Headset Hook", "Gaming", "An under-desk clamp hook that keeps your headset off the desk.", "1100", 42, "PETG", "Black"},
      {"Bookend Pair (Geometric)", "Home & Decor", "A faceted bookend pair heavy enough to hold a full shelf upright.", "2450", 22, "PETG", "Grey"},
    };
    for (Object[] def : defs) {
      Product product = new Product();
      product.setName((String) def[0]);
      product.setCategory(categoryByName.get((String) def[1]));
      product.setDescription((String) def[2]);
      product.setPrice(new BigDecimal((String) def[3]));
      product.setStockQuantity((Integer) def[4]);
      product.setMaterial((String) def[5]);
      product.setColor((String) def[6]);
      product.setImageUrl("https://placehold.co/600x600/1f2937/e5e7eb?text=" + ((String) def[0]).replace(" ", "+"));
      products.save(product);
    }
  }
}
