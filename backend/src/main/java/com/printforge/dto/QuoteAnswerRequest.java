package com.printforge.dto;

import java.math.BigDecimal;

/**
 * A customer accepting a quote.
 *
 * @param expectedQuote the amount the customer saw. If the studio revised the quote since, the
 *                      accept is refused so nobody agrees to a price they were not shown. Optional
 *                      for API clients; the storefront always sends it.
 */
public record QuoteAnswerRequest(BigDecimal expectedQuote) { }
