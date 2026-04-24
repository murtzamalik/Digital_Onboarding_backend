package com.bank.cebos.integration.bbs;

import com.bank.cebos.entity.EmployeeOnboarding;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;

/**
 * Maps BBS {@code /api/extractPakistaniIdCard} to flat fields (same strategy as Android
 * {@code PakistaniCnicOcrMapper}).
 */
public final class PakistaniCnicBbsResponseMapper {

  private PakistaniCnicBbsResponseMapper() {}

  public static void mergeBbsOcrKey(
      EmployeeOnboarding e, String key, JsonNode bbsResponse, ObjectMapper objectMapper) {
    try {
      String existing = e.getOcrExtractedJson();
      ObjectNode root;
      if (StringUtils.hasText(existing)) {
        root = (ObjectNode) objectMapper.readTree(existing);
      } else {
        root = objectMapper.createObjectNode();
      }
      root.set(key, bbsResponse);
      e.setOcrExtractedJson(objectMapper.writeValueAsString(root));
    } catch (Exception ex) {
      throw new IllegalStateException("Failed to merge OCR JSON", ex);
    }
  }

  /** Flattens BBS response for persistence on {@link EmployeeOnboarding} (one side per request). */
  public static OcrTextFields toOcrTextFields(JsonNode resp) {
    if (resp == null) {
      return OcrTextFields.empty();
    }
    JsonNode fe = resp.path("frontSide").path("english");
    if (fe.isObject() && !fe.isEmpty()) {
      return new OcrTextFields(
          text(fe, "name", "holderName", "fullName"),
          text(fe, "father_name", "fatherName", "father", "fathersName"),
          null,
          cnic(
              text(
                  fe,
                  "identity_number",
                  "identityNumber",
                  "cnic",
                  "cnicNumber",
                  "nic",
                  "nationalId",
                  "idCardNumber"),
              resp.path("extractedData")),
          text(fe, "date_of_birth", "dateOfBirth", "dob", "birthDate"),
          text(fe, "gender", "sex"),
          text(
              fe,
              "date_of_issue",
              "dateOfIssue",
              "issueDate",
              "cnicIssueDate",
              "cnic_issue_date",
              "issue"),
          text(
              fe,
              "date_of_expiry",
              "dateOfExpiry",
              "expiryDate",
              "cnicExpiryDate",
              "cnic_expiry_date",
              "validUntil",
              "valid_until"),
          null);
    }
    JsonNode be = resp.path("backSide").path("english");
    if (be.isObject()) {
      return new OcrTextFields(
          null,
          null,
          null,
          cnic(
              text(
                  be,
                  "identity_number",
                  "identityNumber",
                  "cnic",
                  "cnicNumber",
                  "nic",
                  "nationalId"),
              null),
          null,
          null,
          null,
          null,
          addressBlock(
              text(be, "present_address", "presentAddress", "present"),
              text(be, "permanent_address", "permanentAddress", "permanent")));
    }
    return fromFlatExtractedData(resp.get("extractedData"));
  }

  private static OcrTextFields fromFlatExtractedData(JsonNode root) {
    if (root == null || !root.isObject()) {
      return OcrTextFields.empty();
    }
    return new OcrTextFields(
        text(
            root,
            "fullName",
            "full_name",
            "name",
            "personName",
            "person_name",
            "holderName",
            "holder_name"),
        text(root, "fatherName", "father_name", "fathersName", "father", "fathersName"),
        text(root, "motherName", "mother_name", "mothersName", "mother", "mothersName"),
        cnic(
            text(
                root,
                "cnic",
                "cnicNumber",
                "cnic_number",
                "nationalId",
                "identityNumber",
                "nic",
                "idCardNumber",
                "id_card_number",
                "national_id",
                "identity_number"),
            null),
        text(root, "dob", "dateOfBirth", "date_of_birth", "birthDate", "birth_date"),
        text(root, "gender", "sex"),
        text(
            root,
            "issueDate",
            "dateOfIssue",
            "date_of_issue",
            "cnicIssueDate",
            "cnic_issue_date",
            "issue",
            "issue_date"),
        text(
            root,
            "expiryDate",
            "dateOfExpiry",
            "date_of_expiry",
            "cnicExpiryDate",
            "cnic_expiry_date",
            "validUntil",
            "valid_until",
            "expiry",
            "expiry_date"),
        text(
            root,
            "address",
            "presentAddress",
            "present_address",
            "permanentAddress",
            "permanent_address",
            "fullAddress",
            "residentialAddress"));
  }

  private static String text(JsonNode n, String... keys) {
    for (String k : keys) {
      if (!n.has(k) || n.get(k).isNull()) {
        continue;
      }
      String s = asTrimmedString(n.get(k));
      if (s != null) {
        return s;
      }
    }
    return null;
  }

  private static String asTrimmedString(JsonNode n) {
    if (n == null || n.isNull()) {
      return null;
    }
    if (n.isObject()) {
      for (String k : new String[] {"value", "text", "label"}) {
        if (n.has(k) && n.get(k).isTextual()) {
          String t = n.get(k).asText().trim();
          if (StringUtils.hasText(t)) {
            return t;
          }
        }
      }
      return null;
    }
    if (n.isTextual()) {
      return n.asText() != null ? n.asText().trim() : null;
    }
    if (n.isNumber() || n.isBoolean()) {
      return n.asText() != null ? n.asText().trim() : null;
    }
    return null;
  }

  private static String cnic(String fromEnglish, JsonNode flatFallback) {
    if (StringUtils.hasText(fromEnglish)) {
      return fromEnglish;
    }
    if (flatFallback == null || !flatFallback.isObject()) {
      return null;
    }
    return text(
        flatFallback,
        "cnic",
        "cnicNumber",
        "cnic_number",
        "nationalId",
        "identityNumber",
        "nic",
        "idCardNumber",
        "id_card_number",
        "national_id",
        "identity_number");
  }

  private static String addressBlock(String present, String permanent) {
    String p = present != null ? present.trim() : "";
    String m = permanent != null ? permanent.trim() : "";
    if (!p.isEmpty() && !m.isEmpty() && !p.equalsIgnoreCase(m)) {
      return p + "\n" + m;
    }
    if (!p.isEmpty()) {
      return p;
    }
    if (!m.isEmpty()) {
      return m;
    }
    return null;
  }

  public record OcrTextFields(
      String fullName,
      String fatherName,
      String motherName,
      String cnic,
      String dob,
      String gender,
      String issueDate,
      String expiryDate,
      String address) {

    static OcrTextFields empty() {
      return new OcrTextFields(null, null, null, null, null, null, null, null, null);
    }

    public OcrTextFields withMerged(OcrTextFields o) {
      return new OcrTextFields(
          Stream.of(fullName, o.fullName).filter(StringUtils::hasText).findFirst().orElse(null),
          Stream.of(fatherName, o.fatherName).filter(StringUtils::hasText).findFirst().orElse(null),
          Stream.of(motherName, o.motherName).filter(StringUtils::hasText).findFirst().orElse(null),
          Stream.of(cnic, o.cnic).filter(StringUtils::hasText).findFirst().orElse(null),
          Stream.of(dob, o.dob).filter(StringUtils::hasText).findFirst().orElse(null),
          Stream.of(gender, o.gender).filter(StringUtils::hasText).findFirst().orElse(null),
          Stream.of(issueDate, o.issueDate).filter(StringUtils::hasText).findFirst().orElse(null),
          Stream.of(expiryDate, o.expiryDate).filter(StringUtils::hasText).findFirst().orElse(null),
          Stream.of(address, o.address).filter(StringUtils::hasText).findFirst().orElse(null));
    }
  }
}
