package com.bank.cebos.service.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Shared validation rules for bulk Excel rows (initial upload and corrections). */
public final class BulkUploadRowRules {

  private static final Pattern CNIC_PATTERN = Pattern.compile("^\\d{5}-\\d{7}-\\d$");

  private BulkUploadRowRules() {}

  public static List<String> validate(String cnic, String mobile, String fullName) {
    List<String> errors = new ArrayList<>();
    String c = cnic == null ? "" : cnic.trim();
    String m = mobile == null ? "" : mobile.trim();
    String n = fullName == null ? "" : fullName.trim();
    if (n.isEmpty()) {
      errors.add("FULL_NAME is required");
    }
    if (m.isEmpty()) {
      errors.add("MOBILE is required");
    } else if (m.length() < 10) {
      errors.add("MOBILE looks too short");
    }
    if (c.isEmpty()) {
      errors.add("CNIC is required");
    } else {
      String normalized = c.replaceAll("\\s+", "");
      if (!CNIC_PATTERN.matcher(normalized).matches()) {
        errors.add("CNIC must match #####-#######-#");
      }
    }
    return errors;
  }

  public static String normalizeCnicKey(String cnic) {
    if (cnic == null) {
      return "";
    }
    return cnic.replaceAll("\\s+", "");
  }
}
