package com.github.yuu1111.protocol;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

/**
 * MCPプロトコルリクエスト (JSON-RPC 2.0準拠) Java 21のRecordを使用した不変データクラス
 */
public record MCPRequest(@JsonProperty("jsonrpc") String jsonrpc, @JsonProperty("id") String id,
                         @JsonProperty("method") String method,
                         @JsonProperty("params") Map<String, Object> params) {

  /**
   * MCPリクエストのファクトリメソッド
   */
  public static MCPRequest create(String id, String method, Map<String, Object> params) {
    return new MCPRequest("2.0", id, method, params);
  }

  /**
   * バリデーション
   */
  public boolean isValid() {
    return "2.0".equals(jsonrpc) && id != null && !id.isEmpty() && method != null
        && !method.isEmpty();
  }

  /**
   * ツール実行リクエストかどうかを判定
   */
  public boolean isToolExecution() {
    return method != null && method.startsWith("tools/");
  }

  /**
   * ツール名を取得
   */
  public String getToolName() {
    if (!isToolExecution()) {
      return null;
    }
    return method.substring("tools/".length());
  }
}