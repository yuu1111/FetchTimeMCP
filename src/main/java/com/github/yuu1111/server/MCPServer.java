package com.github.yuu1111.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.yuu1111.protocol.*;
import com.github.yuu1111.tools.MCPTool;
import com.github.yuu1111.tools.ToolExecutionException;
import com.github.yuu1111.tools.ToolResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MCPサーバー実装
 * WebSocketとHTTPの両方をサポート
 * Java 21のVirtual Threadsを使用した高性能実装
 */
public class MCPServer {
    private static final Logger logger = LoggerFactory.getLogger(MCPServer.class);
    
    private final int port;
    private final Server server;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;
    private final ServerConfig config;
    
    /**
     * コンストラクタ
     */
    public MCPServer(ServerConfig config) {
        this.config = config;
        this.port = config.port();
        this.server = new Server();
        this.toolRegistry = new ToolRegistry();
        this.objectMapper = createObjectMapper();
        // Java 21のVirtual Threadsを使用
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        
        configureServer();
    }
    
    /**
     * シンプルなコンストラクタ（デフォルト設定）
     */
    public MCPServer(int port) {
        this(ServerConfig.builder().port(port).build());
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
     * サーバーの設定
     */
    private void configureServer() {
        // HTTPコネクタの設定
        ServerConnector connector = new ServerConnector(server);
        connector.setPort(port);
        connector.setHost(config.host());
        connector.setIdleTimeout(config.idleTimeout());
        server.addConnector(connector);
        
        // Servletコンテキストの設定
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        server.setHandler(context);
        
        // HTTPエンドポイントの追加
        context.addServlet(new ServletHolder(new MCPHttpServlet()), "/mcp/*");
        
        // WebSocketエンドポイントの設定
        if (config.enableWebSocket()) {
            JettyWebSocketServletContainerInitializer.configure(context, (servletContext, container) -> {
                container.setMaxTextMessageSize(config.maxMessageSize());
                container.setIdleTimeout(Duration.ofMillis(config.idleTimeout()));
                container.addMapping("/mcp/ws", (req, resp) -> new MCPWebSocketHandler(this));
            });
        }
        
        logger.info("MCPServer configured on {}:{}", config.host(), port);
    }
    
    /**
     * サーバーを起動
     */
    public void start() throws Exception {
        logger.info("Starting MCP Server on port {}", port);
        server.start();
        logger.info("MCP Server started successfully");
        
        // シャットダウンフックの登録
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
        }));
    }
    
    /**
     * サーバーを停止
     */
    public void stop() throws Exception {
        logger.info("Stopping MCP Server");
        executor.shutdown();
        server.stop();
        logger.info("MCP Server stopped");
    }
    
    /**
     * サーバーが起動しているか確認
     */
    public boolean isRunning() {
        return server.isRunning();
    }
    
    /**
     * ツールを登録
     */
    public void registerTool(MCPTool tool) {
        toolRegistry.register(tool);
        logger.info("Registered tool: {}", tool.getName());
    }
    
    /**
     * MCPリクエストを処理
     */
    public MCPResponse processRequest(MCPRequest request) {
        logger.debug("Processing request: {}", request);
        
        // リクエストのバリデーション
        if (!request.isValid()) {
            return MCPResponse.error(request.id(), MCPError.invalidRequest("Invalid request format"));
        }
        
        try {
            // ツール実行リクエストの処理
            if (request.isToolExecution()) {
                return executeToolRequest(request);
            }
            
            // その他のメソッドの処理
            return switch (request.method()) {
                case "tools/list" -> handleListTools(request);
                case "ping" -> handlePing(request);
                case "server/info" -> handleServerInfo(request);
                default -> MCPResponse.error(request.id(), MCPError.methodNotFound(request.method()));
            };
            
        } catch (Exception e) {
            logger.error("Error processing request", e);
            return MCPResponse.error(request.id(), MCPError.internalError(e.getMessage()));
        }
    }
    
    /**
     * ツール実行リクエストを処理
     */
    private MCPResponse executeToolRequest(MCPRequest request) {
        String toolName = request.getToolName();
        MCPTool tool = toolRegistry.getTool(toolName);
        
        if (tool == null) {
            return MCPResponse.error(request.id(), MCPError.methodNotFound("Tool not found: " + toolName));
        }
        
        // パラメータの検証
        MCPError validationError = tool.validateParameters(request.params());
        if (validationError != null) {
            return MCPResponse.error(request.id(), validationError);
        }
        
        try {
            // ツールを実行
            ToolResponse response = tool.execute(request.params());
            return MCPResponse.success(request.id(), response.data());
            
        } catch (ToolExecutionException e) {
            logger.error("Tool execution failed: {}", toolName, e);
            return MCPResponse.error(request.id(), e.getMcpError());
        } catch (Exception e) {
            logger.error("Unexpected error executing tool: {}", toolName, e);
            return MCPResponse.error(request.id(), MCPError.internalError(e.getMessage()));
        }
    }
    
    /**
     * tools/listハンドラ
     */
    private MCPResponse handleListTools(MCPRequest request) {
        var tools = toolRegistry.getAllTools();
        return MCPResponse.success(request.id(), Map.of("tools", tools));
    }
    
    /**
     * pingハンドラ
     */
    private MCPResponse handlePing(MCPRequest request) {
        return MCPResponse.success(request.id(), Map.of("pong", true, "timestamp", System.currentTimeMillis()));
    }
    
    /**
     * server/infoハンドラ
     */
    private MCPResponse handleServerInfo(MCPRequest request) {
        return MCPResponse.success(request.id(), Map.of(
            "name", "FetchTimeMCP",
            "version", "1.0.0",
            "protocol", "MCP/2.0",
            "capabilities", Map.of(
                "tools", true,
                "websocket", config.enableWebSocket(),
                "caching", config.enableCaching()
            )
        ));
    }
    
    /**
     * HTTPサーブレット実装
     */
    private class MCPHttpServlet extends HttpServlet {
        @Override
        protected void doPost(@NotNull HttpServletRequest req, @NotNull HttpServletResponse resp) throws IOException {
            // Content-Typeチェック
            if (!"application/json".equals(req.getContentType())) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.getWriter().write("{\"error\":\"Content-Type must be application/json\"}");
                return;
            }
            
            try {
                // リクエストを読み込み
                MCPRequest mcpRequest = objectMapper.readValue(req.getInputStream(), MCPRequest.class);
                
                // リクエストを処理
                MCPResponse mcpResponse = processRequest(mcpRequest);
                
                // レスポンスを返す
                resp.setContentType("application/json");
                resp.setStatus(HttpServletResponse.SC_OK);
                objectMapper.writeValue(resp.getOutputStream(), mcpResponse);
                
            } catch (Exception e) {
                logger.error("Error handling HTTP request", e);
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getWriter().write("{\"error\":\"Internal server error\"}");
            }
        }
    }
    
    /**
     * ツールレジストリを取得
     */
    public ToolRegistry getToolRegistry() {
        return toolRegistry;
    }
    
    /**
     * ObjectMapperを取得
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}