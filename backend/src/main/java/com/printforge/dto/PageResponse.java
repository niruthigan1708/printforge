package com.printforge.dto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.function.Function;

/** One page of a list endpoint. {@code page} is zero-based. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
  public static final int DEFAULT_SIZE = 20;
  public static final int MAX_SIZE = 100;

  public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
    return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
  }

  /** Validated request for the newest items first (ties broken by id so pages never overlap). */
  public static Pageable newestFirst(int page, int size) {
    if (page < 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be 0 or greater");
    if (size < 1 || size > MAX_SIZE) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and " + MAX_SIZE);
    return PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
  }
}
