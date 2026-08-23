# 视觉重构与 WorkBuddy AI 接入技术方案

## 架构

`Kuikly UI → ChatController → ChatRepository → BridgeModule → KRBridgeModule → 合作方 HTTPS 代理 → WorkBuddy OpenAPI`。

通用层负责业务状态、请求模型、响应结构化和全部 UI；Android 宿主负责网络 I/O、超时、HTTP 状态与 JSON 边界。合作方后端负责 OAuth 票据、WorkBuddy Assistant/Task 生命周期及票据刷新。

## 数据流

1. 用户发送问题，Controller 立即追加用户消息并进入发送态。
2. Repository 将问题、已有 conversation_id 和当前 Fixture 行情摘要交给 Native Bridge。
3. Android 在单线程 Executor 中调用代理，完成后切回主线程回调。
4. Controller 将回答解析为 Markdown 与结构化股票块，记录新的 conversation_id。
5. 错误转换为失败消息，保留原问题供重试。

## 安全与可靠性

- 代理地址由 `WORKBUDDY_PROXY_URL` 环境变量在构建时注入，默认空字符串。
- 仅允许 HTTPS；Debug 可用显式配置的开发地址时仍需自行修改策略，本仓库默认不开放明文。
- 请求设置连接与读取超时，不记录请求正文或响应中的潜在敏感信息。
- Activity 仍仅导出 Launcher 入口，不新增导出组件。

## 视觉实现

使用 `DesignTokens` 作为唯一原值来源。应用壳采用夜空深靛蓝层级；粉色用于主操作和选中态，霓虹青用于连接态、描边感和辅助高光，股票涨跌保持红/绿并加方向文本。

## 实现状态

- T001–T017 的代码与文档实现已完成。
- 业务 UI 未引入原生 Android 业务视图，继续使用 Kuikly UI DSL。
- DESIGN.md 色板、字号与圆角由 `DesignTokens` 统一承载，业务组件不保留旧灰蓝色值或旧语义兼容别名。
- 正式首页默认使用 `WorkBuddyChatRepository`。
- 测试与构建属于后续 Stage 3/4，本方案不预先记录通过结论。
