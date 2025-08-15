package com.github.yuu1111;

import com.github.yuu1111.server.StdioMCPServer;
import com.github.yuu1111.tools.impl.GetCurrentTime;
import com.github.yuu1111.tools.impl.ConvertTimezone;
import com.github.yuu1111.tools.impl.GetReligiousCalendar;
import com.github.yuu1111.tools.impl.GetAstronomicalInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stdio方式のMCPサーバーメインクラス
 * Claude Codeとの通信用
 */
public class StdioMain {
    
    private static final Logger logger = LoggerFactory.getLogger(StdioMain.class);
    
    public static void main(String[] args) {
        try {
            // ロギングをファイルに出力（標準出力を使わない）
            System.setProperty("logback.configurationFile", "logback-stdio.xml");
            
            // StdioMCPサーバーを作成
            StdioMCPServer server = new StdioMCPServer();
            
            // ツールを登録
            registerTools(server);
            
            // サーバーを起動（標準入出力で通信）
            server.start();
            
        } catch (Exception e) {
            logger.error("Failed to start Stdio MCP Server", e);
            System.exit(1);
        }
    }
    
    /**
     * ツールを登録
     */
    private static void registerTools(StdioMCPServer server) {
        // Phase 1: 基本ツール
        server.registerTool(new GetCurrentTime());
        server.registerTool(new ConvertTimezone());
        
        // Phase 2: 高度な機能
        server.registerTool(new GetReligiousCalendar());
        server.registerTool(new GetAstronomicalInfo());
        
        logger.info("Registered {} tools", server.getToolRegistry().size());
    }
}