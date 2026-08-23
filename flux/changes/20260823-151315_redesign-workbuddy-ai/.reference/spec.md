# 视觉重构与 WorkBuddy AI 接入规格

## 功能要求

1. 所有业务页面继续使用 Kuikly UI DSL，并统一消费 `DesignTokens`。
2. 根背景为 `#060318`，主表面为 `#0D0C22`，浮层为 `#151338`；主 CTA 使用 `#FF3366`，辅助强调使用 `#00E5FF`，红橙 `#FF6347` 只作少量提醒。
3. 行情页应包含品牌头部、市场摘要、分类 Chip、股票卡片列表、加载/空/错状态；去除面向开发者的 Demo 状态按钮。
4. 股票详情应包含返回、主价格、涨跌方向文本、关键指标、趋势、AI 洞察、风险与免责声明。
5. AI 页应包含 WorkBuddy 连接状态、欢迎引导、建议问题、完整会话、Markdown、股票卡片、趋势和输入发送。
6. AI 请求必须异步，发送中防重；失败消息支持原问题重试；成功后保存 conversation_id 用于续接。
7. Android 端仅调用 `WORKBUDDY_PROXY_URL` 指定的 HTTPS 合作方后端。客户端不得包含 WorkBuddy access_token、refresh_token 或 client_secret。
8. 代理请求 JSON：`question`、可选 `conversation_id`、`context.quotes`；响应 JSON：`answer`、可选 `conversation_id`、可选 `symbols` 与 `show_trend`。
9. 未配置代理地址、非 HTTPS 地址、超时、非 2xx 和无效 JSON 都必须返回可读错误。

## 验收标准

- 代码中旧蓝灰 Token 不再作为业务视觉主色。
- Android Debug APK 可构建；现有与新增单元测试全部通过。
- 无密钥或 Token 写入版本库。
- 未配置后端时 UI 明确显示“待连接”，发送后给出配置提示。
- 配置合法代理后，聊天请求经 Native Module 异步执行并更新 Kuikly 状态。
- 涨跌信息同时使用颜色和文字/符号表达。

## 实现追踪

| 规格范围 | 实现任务 | 实现位置 | 状态 |
|---|---|---|---|
| Kuikly UI 与 DESIGN.md Token | T003、T006–T008、T014–T015、T017 | `DesignTokens.kt`、`StockHomePage.kt`、`ui/component/` | 已实现，待 Stage 3/4 验证 |
| WorkBuddy 异步会话与续接 | T004–T005、T009–T013 | `domain/`、`presentation/`、`data/WorkBuddyChatRepository.kt`、Native Bridge | 已实现，待 Stage 3/4 验证 |
| HTTPS 配置与凭证边界 | T001–T002、T011–T012、T016 | Android 构建配置、Manifest、Bridge、README | 已实现，待 Stage 3/4 验证 |
| 正式首页默认 WorkBuddy | T013、T017 | `StockHomePage.kt` | 已实现，待 Stage 3/4 验证 |
