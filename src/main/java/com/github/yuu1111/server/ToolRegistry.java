package com.github.yuu1111.server;

import com.github.yuu1111.tools.MCPTool;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ツールレジストリ MCPツールの登録と管理を行う
 */
public class ToolRegistry {

  private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);

  private final Map<String, MCPTool> tools = new ConcurrentHashMap<>();
  private final Map<String, ToolMetadata> metadata = new ConcurrentHashMap<>();

  /**
   * ツールを登録
   */
  public void register(MCPTool tool) {
    if (tool == null) {
      throw new IllegalArgumentException("Tool cannot be null");
    }

    String name = tool.getName();
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Tool name cannot be null or empty");
    }

    tools.put(name, tool);
    metadata.put(name, createMetadata(tool));
    logger.info("Registered tool: {} - {}", name, tool.getDescription());
  }

  /**
   * 複数のツールを一括登録
   */
  public void registerAll(Collection<MCPTool> toolsToRegister) {
    toolsToRegister.forEach(this::register);
  }

  /**
   * ツールを取得
   */
  public MCPTool getTool(String name) {
    return tools.get(name);
  }

  /**
   * ツールが存在するか確認
   */
  public boolean hasTool(String name) {
    return tools.containsKey(name);
  }

  /**
   * ツールを削除
   */
  public void unregister(String name) {
    MCPTool removed = tools.remove(name);
    if (removed != null) {
      metadata.remove(name);
      logger.info("Unregistered tool: {}", name);
    }
  }

  /**
   * すべてのツール名を取得
   */
  public Set<String> getToolNames() {
    return new HashSet<>(tools.keySet());
  }

  /**
   * すべてのツールを取得
   */
  public Collection<MCPTool> getTools() {
    return new ArrayList<>(tools.values());
  }

  /**
   * すべてのツール情報を取得（MCP tools/list用）
   */
  public List<Map<String, Object>> getAllTools() {
    return tools.entrySet().stream().map(entry -> {
      MCPTool tool = entry.getValue();
      Map<String, Object> toolInfo = new HashMap<>();
      toolInfo.put("name", tool.getName());
      toolInfo.put("description", tool.getDescription());
      toolInfo.put("parameters", tool.getParameterSchema());

      // メタデータを追加
      ToolMetadata meta = metadata.get(entry.getKey());
      if (meta != null) {
        toolInfo.put("cacheable", meta.cacheable());
        toolInfo.put("cacheTTL", meta.cacheTTL());
        toolInfo.put("category", meta.category());
        toolInfo.put("version", meta.version());
      }

      return toolInfo;
    }).collect(Collectors.toList());
  }

  /**
   * カテゴリ別にツールを取得
   */
  public Map<String, List<String>> getToolsByCategory() {
    Map<String, List<String>> result = new HashMap<>();

    metadata.forEach((name, meta) -> {
      String category = meta.category();
      result.computeIfAbsent(category, k -> new ArrayList<>()).add(name);
    });

    return result;
  }

  /**
   * キャッシュ可能なツールを取得
   */
  public List<String> getCacheableTools() {
    return metadata.entrySet().stream().filter(entry -> entry.getValue().cacheable())
        .map(Map.Entry::getKey).collect(Collectors.toList());
  }

  /**
   * ツールのメタデータを作成
   */
  private ToolMetadata createMetadata(MCPTool tool) {
    // ツールのカテゴリを判定
    String category = determineCategory(tool);

    return new ToolMetadata(tool.getName(), tool.getDescription(), category, "1.0.0",
        tool.isCacheable(), tool.getCacheTTL());
  }

  /**
   * ツールのカテゴリを判定
   */
  private String determineCategory(MCPTool tool) {
    String name = tool.getName().toLowerCase();

    if (name.contains("time") || name.contains("timezone") || name.contains("clock")) {
      return "time";
    } else if (name.contains("calendar") || name.contains("date")) {
      return "calendar";
    } else if (name.contains("astro") || name.contains("sun") || name.contains("moon")) {
      return "astronomy";
    } else if (name.contains("holiday") || name.contains("festival")) {
      return "holiday";
    } else {
      return "general";
    }
  }

  /**
   * レジストリをクリア
   */
  public void clear() {
    tools.clear();
    metadata.clear();
    logger.info("Tool registry cleared");
  }

  /**
   * 登録されているツール数を取得
   */
  public int size() {
    return tools.size();
  }

  /**
   * レジストリが空か確認
   */
  public boolean isEmpty() {
    return tools.isEmpty();
  }

  /**
   * ツールメタデータ
   */
  private record ToolMetadata(String name, String description, String category, String version,
                              boolean cacheable, int cacheTTL) {

  }
}