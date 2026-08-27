package com.printforge.service;

import com.printforge.dto.CustomPrintRequestResponse;
import com.printforge.dto.QuoteRequest;
import com.printforge.entity.CustomPrintRequest;
import com.printforge.entity.RequestStatus;
import com.printforge.entity.User;
import com.printforge.repository.CustomPrintRequestRepository;
import com.printforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Set;

@Service @RequiredArgsConstructor
public class CustomPrintService {
  private static final Set<String> MATERIALS = Set.of("PLA", "PETG", "ABS");
  private final CustomPrintRequestRepository requests;
  private final UserRepository users;
  private final FileStorageService fileStorage;

  @Transactional
  public CustomPrintRequestResponse create(String email, MultipartFile file, String material, String color, int quantity, String notes) {
    if (quantity < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
    if (material == null || !MATERIALS.contains(material.toUpperCase())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material must be one of PLA, PETG, or ABS");
    if (color == null || color.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Color is required");
    User customer = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    FileStorageService.StoredFile stored = fileStorage.store(file);
    CustomPrintRequest request = new CustomPrintRequest();
    request.setCustomer(customer);
    request.setRequestNumber("CR-" + (1001 + requests.count()));
    request.setFileName(stored.originalName());
    request.setFilePath(stored.storedPath());
    request.setFileType(stored.extension());
    request.setMaterial(material);
    request.setColor(color);
    request.setQuantity(quantity);
    request.setNotes(notes);
    return CustomPrintRequestResponse.from(requests.save(request));
  }

  public List<CustomPrintRequestResponse> mine(String email) {
    User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    return requests.findByCustomerOrderByCreatedAtDesc(user).stream().map(CustomPrintRequestResponse::from).toList();
  }

  public CustomPrintRequestResponse getMine(String email, Long id) {
    User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    return CustomPrintRequestResponse.from(requests.findByIdAndCustomer(id, user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom print request not found")));
  }

  @Transactional
  public CustomPrintRequestResponse accept(String email, Long id) {
    User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    CustomPrintRequest request = requests.findByIdAndCustomer(id, user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom print request not found"));
    if (request.getStatus() != RequestStatus.QUOTED) throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a quoted request can be accepted");
    request.setStatus(RequestStatus.ACCEPTED);
    return CustomPrintRequestResponse.from(request);
  }

  @Transactional
  public CustomPrintRequestResponse reject(String email, Long id) {
    User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    CustomPrintRequest request = requests.findByIdAndCustomer(id, user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom print request not found"));
    if (request.getStatus() != RequestStatus.QUOTED) throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a quoted request can be rejected");
    request.setStatus(RequestStatus.REJECTED);
    return CustomPrintRequestResponse.from(request);
  }

  public List<CustomPrintRequestResponse> all() {
    return requests.findAllByOrderByCreatedAtDesc().stream().map(CustomPrintRequestResponse::from).toList();
  }

  public CustomPrintRequest findEntity(Long id) {
    return requests.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom print request not found"));
  }

  public CustomPrintRequestResponse get(Long id) {
    return CustomPrintRequestResponse.from(findEntity(id));
  }

  @Transactional
  public CustomPrintRequestResponse quote(Long id, QuoteRequest quoteRequest) {
    CustomPrintRequest request = findEntity(id);
    if (request.getStatus() == RequestStatus.CANCELLED || request.getStatus() == RequestStatus.COMPLETED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot quote a request that is already " + request.getStatus());
    }
    request.setAdminQuote(quoteRequest.amount());
    request.setAdminNotes(quoteRequest.notes());
    request.setStatus(RequestStatus.QUOTED);
    return CustomPrintRequestResponse.from(request);
  }

  @Transactional
  public CustomPrintRequestResponse updateStatus(Long id, RequestStatus status) {
    CustomPrintRequest request = findEntity(id);
    if (request.getStatus() == RequestStatus.COMPLETED || request.getStatus() == RequestStatus.CANCELLED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change the status of a " + request.getStatus() + " request");
    }
    request.setStatus(status);
    return CustomPrintRequestResponse.from(request);
  }
}
