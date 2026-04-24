package com.bank.cebos.logging;

import com.bank.cebos.config.HttpLoggingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;

/** Formats HTTP bodies for logging (JSON pretty-print / scalar truncation). */
public final class HttpLogPayloadFormatter {

  private HttpLogPayloadFormatter() {}

  public static String formatBodyForLog(
      byte[] content,
      String contentTypeHeader,
      ObjectMapper objectMapper,
      HttpLoggingProperties props) {
    if (content == null || content.length == 0) {
      return "";
    }
    Charset charset = charsetFromContentType(contentTypeHeader);
    String raw = new String(content, charset);
    int maxChars = Math.max(1, props.getMaxLoggedBodyChars());
    if (raw.length() > maxChars) {
      return raw.substring(0, maxChars) + "…(truncated totalChars=" + raw.length() + ")";
    }
    if (!isJsonMediaType(contentTypeHeader)) {
      return raw;
    }
    try {
      JsonNode tree = objectMapper.readTree(raw);
      int maxScalar = props.getMaxJsonScalarChars();
      if (maxScalar > 0) {
        tree = shortenLongStrings(tree.deepCopy(), maxScalar);
      }
      return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(tree);
    } catch (Exception e) {
      return raw;
    }
  }

  public static String formatJsonBytesForLog(
      byte[] jsonUtf8, ObjectMapper objectMapper, HttpLoggingProperties props) {
    return formatBodyForLog(
        jsonUtf8, MediaType.APPLICATION_JSON_VALUE, objectMapper, props);
  }

  private static JsonNode shortenLongStrings(JsonNode node, int maxScalar) {
    if (node == null || node.isNull()) {
      return node;
    }
    if (node.isTextual()) {
      String t = node.asText();
      if (t.length() <= maxScalar) {
        return node;
      }
      String preview = t.substring(0, Math.min(maxScalar, t.length()));
      return TextNode.valueOf(
          "(string,len="
              + t.length()
              + ",preview="
              + preview
              + (t.length() > preview.length() ? "…)" : ")"));
    }
    if (node.isArray()) {
      ArrayNode arr = (ArrayNode) node;
      for (int i = 0; i < arr.size(); i++) {
        arr.set(i, shortenLongStrings(arr.get(i), maxScalar));
      }
      return arr;
    }
    if (node.isObject()) {
      ObjectNode obj = (ObjectNode) node;
      var it = obj.fields();
      while (it.hasNext()) {
        var e = it.next();
        obj.set(e.getKey(), shortenLongStrings(e.getValue(), maxScalar));
      }
      return obj;
    }
    return node;
  }

  private static boolean isJsonMediaType(String contentTypeHeader) {
    if (!StringUtils.hasText(contentTypeHeader)) {
      return false;
    }
    String ct = contentTypeHeader.toLowerCase();
    return ct.contains("json") || ct.endsWith("+json");
  }

  private static Charset charsetFromContentType(String contentTypeHeader) {
    if (!StringUtils.hasText(contentTypeHeader)) {
      return StandardCharsets.UTF_8;
    }
    try {
      MediaType mt = MediaType.parseMediaType(contentTypeHeader.split(";")[0].trim());
      Charset cs = mt.getCharset();
      return cs != null ? cs : StandardCharsets.UTF_8;
    } catch (Exception e) {
      return StandardCharsets.UTF_8;
    }
  }
}
