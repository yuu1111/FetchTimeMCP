package com.github.yuu1111.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.yuu1111.protocol.MCPError;
import com.github.yuu1111.protocol.MCPRequest;
import com.github.yuu1111.protocol.MCPResponse;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WebSocketハンドラー MCPプロトコルのWebSocket通信を処理
 */
public class MCPWebSocketHandler extends WebSocketAdapter {

  private static final Logger logger = LoggerFactory.getLogger(MCPWebSocketHandler.class);

  private final MCPServer server;
  private final ObjectMapper objectMapper;
  private Session session;

  /**
   * コンストラクタ
   */
  public MCPWebSocketHandler(MCPServer server) {
    this.server = server;
    this.objectMapper = server.getObjectMapper();
  }

  /**
   * WebSocket接続時の処理
   */
  @Override
  public void onWebSocketConnect(@NotNull Session session) {
    super.onWebSocketConnect(session);
    this.session = session;
    logger.info("WebSocket connected from: {}", session.getRemoteAddress());

    // 接続成功メッセージを送信
    sendMessage(MCPResponse.success("connection",
        java.util.Map.of("status", "connected", "protocol", "MCP/2.0", "timestamp",
            System.currentTimeMillis())));
  }

  /**
   * WebSocketメッセージ受信時の処理
   */
  @Override
  public void onWebSocketText(@NotNull String message) {
    logger.debug("Received WebSocket message: {}", message);

    // 非同期でメッセージを処理
    CompletableFuture.runAsync(() -> {
      try {
        // JSONをパース
        MCPRequest request = objectMapper.readValue(message, MCPRequest.class);

        // リクエストを処理
        MCPResponse response = server.processRequest(request);

        // レスポンスを送信
        sendMessage(response);

      } catch (IOException e) {
        logger.error("Failed to parse message", e);
        sendError(null, MCPError.parseError(e.getMessage()));
      } catch (Exception e) {
        logger.error("Error processing WebSocket message", e);
        sendError(null, MCPError.internalError(e.getMessage()));
      }
    });
  }

  /**
   * WebSocketクローズ時の処理
   */
  @Override
  public void onWebSocketClose(int statusCode, String reason) {
    super.onWebSocketClose(statusCode, reason);
    logger.info("WebSocket closed: {} - {}", statusCode, reason);
    this.session = null;
  }

  /**
   * WebSocketエラー時の処理
   */
  @Override
  public void onWebSocketError(@NotNull Throwable cause) {
    logger.error("WebSocket error", cause);
  }

  /**
   * メッセージを送信
   */
  private void sendMessage(MCPResponse response) {
    if (session == null || !session.isOpen()) {
      logger.warn("Cannot send message: session is closed");
      return;
    }

    try {
      String json = objectMapper.writeValueAsString(response);
      session.getRemote().sendString(json);
      logger.debug("Sent WebSocket response: {}", json);
    } catch (IOException e) {
      logger.error("Failed to send message", e);
    }
  }

  /**
   * エラーメッセージを送信
   */
  private void sendError(String id, MCPError error) {
    sendMessage(MCPResponse.error(id != null ? id : "unknown", error));
  }

  /**
   * セッションが開いているか確認
   */
  public boolean isOpen() {
    return session != null && session.isOpen();
  }

  /**
   * セッションを閉じる
   */
  public void close() {
    if (session != null && session.isOpen()) {
      session.close();
    }
  }
}