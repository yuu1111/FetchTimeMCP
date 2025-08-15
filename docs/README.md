# FetchTimeMCP ドキュメント

## 📚 ドキュメント一覧

### 開発ドキュメント
- [**仕様書**](./SPECIFICATION.md) - システム全体の詳細仕様
- [**API仕様**](./API.md) - MCPツールのAPI仕様（作成予定）
- [**アーキテクチャ**](./ARCHITECTURE.md) - システム設計詳細（作成予定）

### 実装ガイド
- [**開発ガイド**](./DEVELOPMENT.md) - 開発環境構築とコーディング規約（作成予定）
- [**テストガイド**](./TESTING.md) - テスト戦略と実行方法（作成予定）
- [**デプロイガイド**](./DEPLOYMENT.md) - デプロイ手順（作成予定）

### API統合
- [**外部API統合ガイド**](./EXTERNAL_APIS.md) - 外部API連携詳細（作成予定）
- [**カレンダー変換仕様**](./CALENDAR_CONVERSION.md) - 各種カレンダー変換ロジック（作成予定）

### ユーザーガイド
- [**クイックスタート**](./QUICKSTART.md) - 初めて使う方向け（作成予定）
- [**使用例**](./EXAMPLES.md) - 実用的な使用例集（作成予定）
- [**FAQ**](./FAQ.md) - よくある質問（作成予定）

## 🏗️ ドキュメント構造

```
docs/
├── README.md              # このファイル
├── SPECIFICATION.md       # 詳細仕様書
├── API.md                # API仕様
├── ARCHITECTURE.md       # アーキテクチャ設計
├── DEVELOPMENT.md        # 開発ガイド
├── TESTING.md           # テストガイド
├── DEPLOYMENT.md        # デプロイガイド
├── EXTERNAL_APIS.md     # 外部API統合
├── CALENDAR_CONVERSION.md # カレンダー変換
├── QUICKSTART.md        # クイックスタート
├── EXAMPLES.md          # 使用例
└── FAQ.md              # FAQ
```

## 📝 ドキュメント作成ガイドライン

### Mermaid図の使用
- システム図、フロー図にはMermaidを使用
- 配色はMaterial Designカラーパレットを基準
- 複雑な図は適切に分割

### 記述スタイル
- 日本語を基本言語とする
- 技術用語は英語表記を併記
- コード例は実行可能な形で記載

### バージョン管理
- 各ドキュメントにバージョンと最終更新日を記載
- 重要な変更はCHANGELOG.mdに記録

## 🔄 更新履歴

| 日付 | バージョン | 内容 |
|------|------------|------|
| 2024-01-15 | 1.0.0 | 初版作成、仕様書完成 |

---

*最終更新: 2024-01-15*