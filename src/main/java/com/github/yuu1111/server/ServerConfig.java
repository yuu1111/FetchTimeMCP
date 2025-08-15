package com.github.yuu1111.server;

/**
 * サーバー設定 Java 21のRecordとBuilderパターンを使用
 */
public record ServerConfig(int port, String host, boolean enableWebSocket, boolean enableCaching,
                           int maxConnections, long idleTimeout, int maxMessageSize,
                           boolean enableMetrics) {

  /**
   * デフォルト値
   */
  public static final int DEFAULT_PORT = 3000;
  public static final String DEFAULT_HOST = "localhost";
  public static final boolean DEFAULT_ENABLE_WEBSOCKET = true;
  public static final boolean DEFAULT_ENABLE_CACHING = true;
  public static final int DEFAULT_MAX_CONNECTIONS = 100;
  public static final long DEFAULT_IDLE_TIMEOUT = 30000; // 30秒
  public static final int DEFAULT_MAX_MESSAGE_SIZE = 65536; // 64KB
  public static final boolean DEFAULT_ENABLE_METRICS = true;

  /**
   * デフォルト設定を作成
   */
  public static ServerConfig defaultConfig() {
    return builder().build();
  }

  /**
   * ビルダーを作成
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * 設定ビルダー
   */
  public static class Builder {

    private int port = DEFAULT_PORT;
    private String host = DEFAULT_HOST;
    private boolean enableWebSocket = DEFAULT_ENABLE_WEBSOCKET;
    private boolean enableCaching = DEFAULT_ENABLE_CACHING;
    private int maxConnections = DEFAULT_MAX_CONNECTIONS;
    private long idleTimeout = DEFAULT_IDLE_TIMEOUT;
    private int maxMessageSize = DEFAULT_MAX_MESSAGE_SIZE;
    private boolean enableMetrics = DEFAULT_ENABLE_METRICS;

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder enableWebSocket(boolean enable) {
      this.enableWebSocket = enable;
      return this;
    }

    public Builder enableCaching(boolean enable) {
      this.enableCaching = enable;
      return this;
    }

    public Builder maxConnections(int max) {
      this.maxConnections = max;
      return this;
    }

    public Builder idleTimeout(long timeout) {
      this.idleTimeout = timeout;
      return this;
    }

    public Builder maxMessageSize(int size) {
      this.maxMessageSize = size;
      return this;
    }

    public Builder enableMetrics(boolean enable) {
      this.enableMetrics = enable;
      return this;
    }

    public ServerConfig build() {
      return new ServerConfig(port, host, enableWebSocket, enableCaching, maxConnections,
          idleTimeout, maxMessageSize, enableMetrics);
    }
  }
}