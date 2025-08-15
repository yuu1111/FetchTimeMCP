package com.github.yuu1111.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.yuu1111.tools.MCPTool;
import com.github.yuu1111.tools.ToolExecutionException;
import com.github.yuu1111.tools.ToolResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stdio（標準入出力）ベースのMCPサーバー実装 Claude Codeとの通信用
 */
public class StdioMCPServer {

  private static final Logger logger = LoggerFactory.getLogger(StdioMCPServer.class);

  private final ToolRegistry toolRegistry;
  private final ObjectMapper objectMapper;
  private final BufferedReader reader;
  private final PrintWriter writer;
  private boolean running = false;

  /**
   * コンストラクタ
   */
  public StdioMCPServer() {
    this.toolRegistry = new ToolRegistry();
    this.objectMapper = createObjectMapper();
    this.reader = new BufferedReader(new InputStreamReader(System.in));
    this.writer = new PrintWriter(System.out, true);
  }

  /**
   * ObjectMapperの設定
   */
  private ObjectMapper createObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  /**
   * サーバーを起動
   */
  public void start() {
    logger.info("Starting Stdio MCP Server");
    running = true;

    // 初期化メッセージを送信
    sendInitializationResponse();

    // メインループ
    while (running) {
      try {
        String line = reader.readLine();
        if (line == null) {
          // EOFに達した場合は終了
          break;
        }

        if (line.trim().isEmpty()) {
          continue;
        }

        // JSONRPCリクエストを処理
        processJsonRpcRequest(line);

      } catch (Exception e) {
        logger.error("Error processing request", e);
        sendErrorResponse(null, -32603, "Internal error: " + e.getMessage());
      }
    }

    logger.info("Stdio MCP Server stopped");
  }

  /**
   * 初期化レスポンスを送信
   */
  private void sendInitializationResponse() {
    Map<String, Object> serverInfo = new HashMap<>();
    serverInfo.put("protocolVersion", "2024-11-05");
    serverInfo.put("capabilities", Map.of("tools", Map.of()));
    serverInfo.put("serverInfo", Map.of("name", "FetchTimeMCP", "version", "1.0.0"));

    sendNotification("initialized", serverInfo);
  }

  /**
   * JSONRPCリクエストを処理
   */
  private void processJsonRpcRequest(String json) {
    try {
      JsonNode requestNode = objectMapper.readTree(json);

      String jsonrpc = requestNode.path("jsonrpc").asText();
      if (!"2.0".equals(jsonrpc)) {
        sendErrorResponse(null, -32600, "Invalid Request: jsonrpc must be 2.0");
        return;
      }

      String id = requestNode.path("id").asText(null);
      String method = requestNode.path("method").asText();
      JsonNode params = requestNode.path("params");

      logger.debug("Received request: method={}, id={}", method, id);

      // メソッドごとの処理
      switch (method) {
        case "initialize":
          handleInitialize(id, params);
          break;
        case "tools/list":
          handleListTools(id);
          break;
        case "tools/call":
          handleToolCall(id, params);
          break;
        case "ping":
          handlePing(id);
          break;
        default:
          sendErrorResponse(id, -32601, "Method not found: " + method);
      }

    } catch (Exception e) {
      logger.error("Error parsing JSON-RPC request", e);
      sendErrorResponse(null, -32700, "Parse error");
    }
  }

  /**
   * initializeハンドラ
   */
  private void handleInitialize(String id, JsonNode params) {
    Map<String, Object> result = new HashMap<>();
    result.put("protocolVersion", "2024-11-05");
    result.put("capabilities", Map.of("tools", Map.of()));
    result.put("serverInfo", Map.of("name", "FetchTimeMCP", "version", "1.0.0"));

    sendResponse(id, result);
  }

  /**
   * tools/listハンドラ
   */
  private void handleListTools(String id) {
    List<Map<String, Object>> tools = new ArrayList<>();

    for (Map<String, Object> toolInfo : toolRegistry.getAllTools()) {
      tools.add(toolInfo);
    }

    sendResponse(id, Map.of("tools", tools));
  }

  /**
   * tools/callハンドラ
   */
  private void handleToolCall(String id, JsonNode params) {
    try {
      String toolName = params.path("name").asText();
      JsonNode arguments = params.path("arguments");

      MCPTool tool = toolRegistry.getTool(toolName);
      if (tool == null) {
        sendErrorResponse(id, -32602, "Tool not found: " + toolName);
        return;
      }

      // パラメータをMapに変換
      Map<String, Object> toolParams = objectMapper.convertValue(arguments, Map.class);

      // ツールを実行
      ToolResponse response = tool.execute(toolParams);

      // 結果を返す
      List<Map<String, Object>> content = new ArrayList<>();
      content.add(Map.of("type", "text", "text", objectMapper.writeValueAsString(response.data())));

      sendResponse(id, Map.of("content", content));

    } catch (ToolExecutionException e) {
      logger.error("Tool execution failed", e);
      sendErrorResponse(id, -32603, "Tool execution error: " + e.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected error in tool call", e);
      sendErrorResponse(id, -32603, "Internal error: " + e.getMessage());
    }
  }

  /**
   * pingハンドラ
   */
  private void handlePing(String id) {
    sendResponse(id, Map.of());
  }

  /**
   * 成功レスポンスを送信
   */
  private void sendResponse(String id, Object result) {
    Map<String, Object> response = new HashMap<>();
    response.put("jsonrpc", "2.0");
    response.put("id", id);
    response.put("result", result);

    sendJson(response);
  }

  /**
   * エラーレスポンスを送信
   */
  private void sendErrorResponse(String id, int code, String message) {
    Map<String, Object> response = new HashMap<>();
    response.put("jsonrpc", "2.0");
    if (id != null) {
      response.put("id", id);
    }
    response.put("error", Map.of("code", code, "message", message));

    sendJson(response);
  }

  /**
   * 通知を送信
   */
  private void sendNotification(String method, Object params) {
    Map<String, Object> notification = new HashMap<>();
    notification.put("jsonrpc", "2.0");
    notification.put("method", method);
    notification.put("params", params);

    sendJson(notification);
  }

  /**
   * JSONを送信
   */
  private void sendJson(Object obj) {
    try {
      String json = objectMapper.writeValueAsString(obj);
      writer.println(json);
      writer.flush();
      logger.debug("Sent: {}", json);
    } catch (Exception e) {
      logger.error("Error sending JSON", e);
    }
  }

  /**
   * ツールを登録
   */
  public void registerTool(MCPTool tool) {
    toolRegistry.register(tool);
    logger.info("Registered tool: {}", tool.getName());
  }

  /**
   * サーバーを停止
   */
  public void stop() {
    running = false;
  }

  /**
   * ツールレジストリを取得
   */
  public ToolRegistry getToolRegistry() {
    return toolRegistry;
  }
}