package com.github.yuu1111;

import com.github.yuu1111.server.MCPServer;
import com.github.yuu1111.server.ServerConfig;
import com.github.yuu1111.tools.impl.GetCurrentTime;
import com.github.yuu1111.tools.impl.ConvertTimezone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * FetchTimeMCP メインクラス
 * MCPサーバーを起動し、時間関連のツールを提供
 */
public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final String VERSION = "1.0.0";
    
    public static void main(String[] args) {
        printBanner();
        
        try {
            // 設定をロード
            Properties properties = loadProperties();
            ServerConfig config = buildServerConfig(properties);
            
            // MCPサーバーを作成
            MCPServer server = new MCPServer(config);
            
            // ツールを登録
            registerTools(server);
            
            // サーバーを起動
            server.start();
            
            logger.info("FetchTimeMCP Server v{} started successfully", VERSION);
            logger.info("Server listening on {}:{}", config.host(), config.port());
            logger.info("WebSocket enabled: {}", config.enableWebSocket());
            logger.info("Caching enabled: {}", config.enableCaching());
            
            // サーバーが停止するまで待機
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("Failed to start FetchTimeMCP Server", e);
            System.exit(1);
        }
    }
    
    /**
     * バナーを表示
     */
    private static void printBanner() {
        System.out.println("""
            ╔══════════════════════════════════════════════════════════╗
            ║     _____     _       _   _____ _            __  __  ___ ║
            ║    |  ___|___| |_ ___| |_|_   _(_)_ __ ___  |  \\/  |/ __|║
            ║    | |_ / _ \\ __/ __| '_ \\| | | | '_ ` _ \\ | |\\/| | |   ║
            ║    |  _|  __/ || (__| | | | | | | | | | | || |  | | |__ ║
            ║    |_|  \\___|\\__\\___|_| |_|_| |_|_| |_| |_||_|  |_|\\___| ║
            ║                                                           ║
            ║           Comprehensive Time & Date MCP Server           ║
            ║                      Version %s                      ║
            ╚══════════════════════════════════════════════════════════╝
            """.formatted(VERSION));
    }
    
    /**
     * プロパティをロード
     */
    private static Properties loadProperties() {
        Properties properties = new Properties();
        
        // デフォルト設定をロード
        try (InputStream input = Main.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                properties.load(input);
                logger.info("Loaded configuration from application.properties");
            } else {
                logger.warn("application.properties not found, using defaults");
            }
        } catch (IOException e) {
            logger.error("Failed to load application.properties", e);
        }
        
        // 環境変数でオーバーライド
        overrideWithEnvironmentVariables(properties);
        
        return properties;
    }
    
    /**
     * 環境変数でプロパティをオーバーライド
     */
    private static void overrideWithEnvironmentVariables(Properties properties) {
        // サーバー設定
        String port = System.getenv("MCP_SERVER_PORT");
        if (port != null) {
            properties.setProperty("server.port", port);
        }
        
        String host = System.getenv("MCP_SERVER_HOST");
        if (host != null) {
            properties.setProperty("server.host", host);
        }
        
        // API設定
        String worldtimeApiKey = System.getenv("WORLDTIME_API_KEY");
        if (worldtimeApiKey != null) {
            properties.setProperty("worldtime.api.key", worldtimeApiKey);
        }
        
        // キャッシュ設定
        String cacheEnabled = System.getenv("CACHE_ENABLED");
        if (cacheEnabled != null) {
            properties.setProperty("cache.enabled", cacheEnabled);
        }
        
        // ログレベル
        String logLevel = System.getenv("LOG_LEVEL");
        if (logLevel != null) {
            properties.setProperty("logging.level.root", logLevel);
        }
    }
    
    /**
     * サーバー設定を構築
     */
    private static ServerConfig buildServerConfig(Properties properties) {
        return ServerConfig.builder()
            .port(Integer.parseInt(properties.getProperty("server.port", "3000")))
            .host(properties.getProperty("server.host", "localhost"))
            .enableWebSocket(Boolean.parseBoolean(
                properties.getProperty("server.enable.websocket", "true")))
            .enableCaching(Boolean.parseBoolean(
                properties.getProperty("server.enable.caching", "true")))
            .maxConnections(Integer.parseInt(
                properties.getProperty("server.max.connections", "100")))
            .idleTimeout(Long.parseLong(
                properties.getProperty("server.idle.timeout", "30000")))
            .maxMessageSize(Integer.parseInt(
                properties.getProperty("server.max.message.size", "65536")))
            .enableMetrics(Boolean.parseBoolean(
                properties.getProperty("server.enable.metrics", "true")))
            .build();
    }
    
    /**
     * ツールを登録
     */
    private static void registerTools(MCPServer server) {
        // 基本ツール
        server.registerTool(new GetCurrentTime());
        server.registerTool(new ConvertTimezone());
        
        // Phase 2以降で追加予定のツール
        // server.registerTool(new GetReligiousCalendar());
        // server.registerTool(new GetAstronomicalInfo());
        // server.registerTool(new GetTimeDifference());
        // server.registerTool(new GetHolidays());
        
        logger.info("Registered {} tools", server.getToolRegistry().size());
    }
}