package com.printforge.service;

import com.printforge.dto.CustomPrintRequestResponse;
import com.printforge.dto.FieldLimits;
import com.printforge.dto.PageResponse;
import com.printforge.dto.QuoteRequest;
import com.printforge.entity.CustomPrintRequest;
import com.printforge.entity.RequestStatus;
import com.printforge.entity.User;
import com.printforge.repository.CustomPrintRequestRepository;
import com.printforge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class CustomPrintService {
  private static final Set<String> MATERIALS = Set.of("PLA", "PETG", "ABS");
  private final CustomPrintRequestRepository requests;
  private final UserRepository users;
  private final FileStorageService fileStorage;

  @Transactional
  public CustomPrintRequestResponse create(String email, MultipartFile file, String material, String color, int quantity, String notes) {
    if (quantity < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1");
    if (quantity > FieldLimits.MAX_QUANTITY) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at most " + FieldLimits.MAX_QUANTITY);
    if (material == null || !MATERIALS.contains(material.toUpperCase())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Material must be one of PLA, PETG, or ABS");
    if (color == null || color.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Color is required");
    // These arrive as multipart form fields, not a validated JSON body, so check lengths here.
    if (color.length() > FieldLimits.COLOR) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Color must be at most " + FieldLimits.COLOR + " characters");
    if (notes != null && notes.length() > FieldLimits.LONG_TEXT) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Notes must be at most " + FieldLimits.LONG_TEXT + " characters");
    User customer = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    FileStorageService.StoredFile stored = fileStorage.store(file);
    // The file is written before the row; if the transaction then fails (here or at commit),
    // remove the file so a failed upload leaves no orphan behind.
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override public void afterCompletion(int status) {
        if (status != STATUS_COMMITTED) fileStorage.delete(stored.storedPath());
      }
    });
    CustomPrintRequest request = new CustomPrintRequest();
    request.setCustomer(customer);
    // Temporary unique value; replaced with the id-based number once the row has an id.
    request.setRequestNumber("CR-PENDING-" + UUID.randomUUID());
    request.setFileName(stored.originalName());
    request.setFilePath(stored.storedPath());
    request.setFileType(stored.extension());
    request.setMaterial(material);
    request.setColor(color);
    request.setQuantity(quantity);
    request.setNotes(notes);
    request = requests.saveAndFlush(request);
    request.setRequestNumber(requestNumberFor(request.getId()));
    return CustomPrintRequestResponse.from(request);
  }

  /** Same id-based scheme as order numbers: unique under concurrency, above any legacy count-based number. */
  static String requestNumberFor(long id) { return "CR-" + (1000 + id); }

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
    request.setUpdatedAt(Instant.now());
    return CustomPrintRequestResponse.from(request);
  }

  @Transactional
  public CustomPrintRequestResponse reject(String email, Long id) {
    User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    CustomPrintRequest request = requests.findByIdAndCustomer(id, user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom print request not found"));
    if (request.getStatus() != RequestStatus.QUOTED) throw new ResponseStatusException(HttpStatus.CONFLICT, "Only a quoted request can be rejected");
    request.setStatus(RequestStatus.REJECTED);
    request.setUpdatedAt(Instant.now());
    return CustomPrintRequestResponse.from(request);
  }

  /** A customer withdrawing their own request before it has been quoted. */
  @Transactional
  public CustomPrintRequestResponse cancelMine(String email, Long id) {
    User user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    CustomPrintRequest request = requests.findByIdAndCustomer(id, user).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom print request not found"));
    if (request.getStatus() == RequestStatus.CANCELLED) return CustomPrintRequestResponse.from(request);
    if (!request.getStatus().customerCanCancel()) {
      String hint = request.getStatus() == RequestStatus.QUOTED ? " You can decline the quote instead." : "";
      throw new ResponseStatusException(HttpStatus.CONFLICT, "A " + request.getStatus() + " request can no longer be cancelled." + hint);
    }
    request.setStatus(RequestStatus.CANCELLED);
    request.setUpdatedAt(Instant.now());
    return CustomPrintRequestResponse.from(request);
  }

  @Transactional(readOnly = true)
  public PageResponse<CustomPrintRequestResponse> all(Pageable pageable) {
    return PageResponse.from(requests.findAll(pageable), CustomPrintRequestResponse::from);
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
    if (!request.getStatus().canBeQuoted()) {
      // Once the customer has accepted or rejected, re-quoting would silently undo their answer.
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot quote a request that is already " + request.getStatus());
    }
    request.setAdminQuote(quoteRequest.amount());
    request.setAdminNotes(quoteRequest.notes());
    request.setStatus(RequestStatus.QUOTED);
    request.setUpdatedAt(Instant.now());
    return CustomPrintRequestResponse.from(request);
  }

  @Transactional
  public CustomPrintRequestResponse updateStatus(Long id, RequestStatus status) {
    CustomPrintRequest request = findEntity(id);
    RequestStatus current = request.getStatus();
    if (current == status) return CustomPrintRequestResponse.from(request);
    if (current.isTerminal()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot change the status of a " + current + " request");
    if (status == RequestStatus.QUOTED) throw new ResponseStatusException(HttpStatus.CONFLICT, "Use the quote action to send a price; it sets the request to QUOTED");
    if (status == RequestStatus.ACCEPTED || status == RequestStatus.REJECTED) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Only the customer can accept or reject a quote");
    }
    if (!current.adminCanTransitionTo(status)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot move a request from " + current + " to " + status);
    request.setStatus(status);
    request.setUpdatedAt(Instant.now());
    return CustomPrintRequestResponse.from(request);
  }
}
