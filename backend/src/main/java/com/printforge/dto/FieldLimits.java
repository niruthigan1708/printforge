package com.printforge.dto;

/**
 * Maximum lengths for user-supplied text, kept within the database column sizes (VARCHAR(255)
 * by default, 2000 where the entity says so). Validating against them turns an over-long value
 * into a clear 400 instead of a database error surfacing as a 500.
 */
public final class FieldLimits {
  private FieldLimits() { }

  public static final int PERSON_NAME = 100;
  public static final int EMAIL = 254;
  public static final int PASSWORD_MAX = 72; // BCrypt ignores bytes beyond 72
  public static final int CATEGORY_NAME = 100;
  public static final int CATEGORY_DESCRIPTION = 255;
  public static final int PRODUCT_NAME = 150;
  public static final int LONG_TEXT = 2000; // product description, print notes, admin notes
  public static final int MATERIAL = 50;
  public static final int COLOR = 50;
  public static final int URL = 2048;
  public static final int PHONE = 30;
  public static final int ADDRESS = 255;
  public static final int CITY = 100;
  public static final int POSTAL_CODE = 20;
  public static final int TRACKING_NUMBER = 100;
  public static final int MAX_QUANTITY = 1000;
  public static final int MAX_ORDER_LINES = 50;
}
