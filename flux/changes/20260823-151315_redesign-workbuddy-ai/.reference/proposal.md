# Proposal: Redesign Kuikly Stock and Integrate WorkBuddy AI

**Feature Directory**: `20260823-151315_redesign-workbuddy-ai`
**Created**: 2026-08-23
**Status**: Done
**Implemented**: Done
**Input**: 按 `artifacts/specs/design/DESIGN.md` 重构 Kuikly 股票客户端视觉，补齐功能并通过合作方后端安全接入 WorkBuddy AI。

- Design reference: `artifacts/specs/design/DESIGN.md`
- WorkBuddy reference: `https://www.workbuddy.link/p/JVM0gKdRyl8k9EgElw5TxA`

---

## Clarification

### User Alignment

- Q: 视觉是否继续沿用现有蓝灰配色？ → A: 否，以用户本次选中的 DESIGN.md 为唯一视觉基准。
- Q: AI 是否继续使用 Fixture？ → A: 否，接入 WorkBuddy；没有完成后端配置时明确展示未连接状态。

### Assumptions

- **WorkBuddy 通过合作方后端代理**: 白皮书明确要求 access_token 与 client_secret 不下发到客户端，Android 仅调用可配置的 HTTPS 代理。
- **代理契约由客户端定义为稳定 JSON 接口**: 请求包含 question、conversation_id 与行情上下文；响应包含 answer、conversation_id 和可选结构化股票元数据。
- **保留行情 Fixture**: 原需求允许行情离线 Fixture；本次只将 AI 对话切换为远端链路，避免扩大到真实行情供应商。
- **移动端适配设计语言而非照搬网页布局**: 保留深靛蓝夜空、霓虹粉/青、16px 卡片圆角、清晰层级，并转换为单列移动端信息流。

---

## Design

### Context

改造前实现已使用 Kuikly UI，但视觉 Token 仍是蓝灰风格，聊天仓库也由同步 Fixture 返回。本变更已将业务视觉迁移到 DESIGN.md 的深靛蓝、粉色与霓虹青体系，并将正式首页聊天仓库切换为 WorkBuddy 异步实现。

### Goals / Non-Goals

**Goals:**

- 统一以 DESIGN.md 的颜色、字号、间距、圆角与视觉层级重做全部 Kuikly 页面。
- 提升首页、行情列表、详情、AI 会话、输入区和状态反馈的完成度。
- 通过 Android 原生桥接调用合作方后端，再由后端安全访问 WorkBuddy。
- 支持会话续接、超时、HTTP/解析错误、重试与未配置提示。
- 保持结构化股票卡片、趋势图和 Markdown 回答。

**Non-Goals:**

- 不在客户端保存 WorkBuddy access_token、refresh_token 或 client_secret。
- 不在本次实现 OAuth 回调后端、真实行情供应商、交易和账户功能。
- 不直接从客户端访问 WorkBuddy OpenAPI。

### Risks / Trade-offs

- 实际 AI 请求依赖合作方后端提供代理 URL；缺少配置时应用会清晰提示，而不是静默使用假数据。
- Kuikly 通用层不能直接使用 Android 网络库，因此采用已有 Native Module 桥接；增加少量宿主代码但保持跨端 UI 边界。
- WorkBuddy 白皮书给出开放平台契约，但未提供本项目的 client_id、助理 ID 或代理地址，因此这些值全部外部配置。

### Impact

影响 DesignTokens、应用壳、行情/详情/聊天组件、聊天领域状态、Repository/Controller、Kuikly BridgeModule、Android Native Module、Manifest、Gradle 配置、README 与测试。

## Complexity Analysis

| Dimension | Score | Rationale |
|-----------|-------|-----------|
| Scope | 3 | 跨 commonMain UI、状态、数据层、Android 宿主与测试，涉及 6 个以上文件 |
| Uncertainty | 3 | WorkBuddy 仅给出平台协议，项目代理地址和服务端实现未知，需要定义安全边界与契约 |
| Dependencies | 3 | 新增远端 HTTP 链路和构建配置，依赖合作方后端与 WorkBuddy |
| Risk | 3 | 修改核心聊天路径和主要视觉，涉及异步线程、错误处理与凭据安全 |
| Novelty | 2 | 可复用现有 Bridge/Repository 模式，但远端 AI 与会话续接是新逻辑 |

**Total Score: 14 / 15**
**Pipeline: Thorough Design**

## Capabilities

### New Capabilities

- `workbuddy-ai-proxy`: 通过合作方 HTTPS 代理发起和续接 WorkBuddy AI 对话。
- `ai-connection-state`: 展示连接状态、未配置、生成中、错误和重试反馈。

### Modified Capabilities

- `design-system`: 切换为 DESIGN.md 的深靛蓝、粉色、红橙与霓虹青视觉系统。
- `market-gallery`: 行情信息改为更鲜明的移动端作品卡片式布局。
- `stock-detail`: 强化价格主视觉、指标层级、AI 洞察与风险表达。
- `ai-research`: 从 Fixture 同步回答改为真实异步远端回答和会话续接。

### Unchanged Capabilities

- `kuikly-ui`: 所有业务界面继续使用 Kuikly UI DSL。
- `offline-market-fixtures`: 行情数据继续使用稳定 Fixture。
- `structured-ai-content`: Markdown、股票卡片与迷你趋势图继续作为结构化内容块展示。

## Implementation Tracking

- T001–T017 已完成实现；业务 UI 保持使用 Kuikly UI DSL。
- 视觉原值集中在 `DesignTokens`，业务组件仅消费 DESIGN.md 对应的语义 Token。
- 正式首页默认装配 `WorkBuddyChatRepository`；`InMemoryChatRepository` 仅保留为测试与离线数据实现，不参与正式首页装配。
- 测试与构建结果不在本阶段声明，留待后续 Stage 3/4 验证。
