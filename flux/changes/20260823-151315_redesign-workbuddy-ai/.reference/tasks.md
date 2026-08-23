# 视觉重构与 WorkBuddy AI 接入任务

## Phase 1: Setup

- [x] T001 添加可选的 WorkBuddy 代理构建配置到 `androidApp/build.gradle.kts`
- [x] T002 更新 Android 网络和主题配置 `androidApp/src/main/AndroidManifest.xml`、`androidApp/src/main/res/values/colors.xml`

## Phase 2: Foundational

- [x] T003 [P] 将 DESIGN.md 的颜色、字号、间距、圆角与尺寸落入 `shared/src/commonMain/kotlin/com/ourcx/kuiklystock/theme/DesignTokens.kt`
- [x] T004 [P] 扩展 AI 会话请求、响应与连接状态模型 `shared/src/commonMain/kotlin/com/ourcx/kuiklystock/domain/ChatModels.kt`、`PageStates.kt`
- [x] T005 将聊天数据接口升级为异步会话协议 `shared/src/commonMain/kotlin/com/ourcx/kuiklystock/data/Repositories.kt`

## Phase 3: User Story 1 - 设计规范一致的行情体验（P1）

**Goal**: 首屏不再呈现灰色开发 Demo，而是符合 DESIGN.md 的深靛蓝与霓虹强调视觉。

**Acceptance**: 根背景、卡片、CTA、选中态、字体层级和圆角匹配设计 Token；行情页没有开发状态切换入口。

- [x] T006 [US1] 重构应用头部与底部导航 `shared/src/commonMain/kotlin/com/ourcx/kuiklystock/StockHomePage.kt`
- [x] T007 [US1] 重构行情摘要、分类 Chip 和股票卡片列表 `shared/src/commonMain/kotlin/com/ourcx/kuiklystock/ui/component/MarketContent.kt`
- [x] T008 [US1] 重构股票详情卡片、指标、趋势与洞察 `shared/src/commonMain/kotlin/com/ourcx/kuiklystock/ui/component/StockDetailContent.kt`

## Phase 4: User Story 2 - WorkBuddy AI 对话（P1）

**Goal**: 用户可向真实 WorkBuddy 助理提问、续接会话并处理失败。

**Acceptance**: 请求通过 HTTPS 代理异步发出，成功展示 Markdown 和结构化股票块，失败可重试，未配置状态明确。

- [x] T009 [US2] 实现异步聊天状态机与会话续接 `shared/src/commonMain/kotlin/com/ourcx/kuiklystock/presentation/ChatController.kt`
- [x] T010 [US2] 实现 Kuikly Native Bridge 聊天仓库 `shared/src/commonMain/kotlin/com/ourcx/kuiklystock/data/WorkBuddyChatRepository.kt`
- [x] T011 [US2] 扩展 Kuikly 原生桥接口 `shared/src/commonMain/kotlin/com/ourcx/kuiklystock/base/BridgeModule.kt`
- [x] T012 [US2] 实现 Android HTTPS 代理调用 `androidApp/src/main/java/com/ourcx/kuiklystock/module/KRBridgeModule.kt`
- [x] T013 [US2] 在首页装配 WorkBuddy Repository `shared/src/commonMain/kotlin/com/ourcx/kuiklystock/StockHomePage.kt`
- [x] T014 [US2] 重构 AI 连接状态、欢迎页和会话卡片 `shared/src/commonMain/kotlin/com/ourcx/kuiklystock/ui/component/AiResearchContent.kt`
- [x] T015 [US2] 重构聊天输入与发送反馈 `shared/src/commonMain/kotlin/com/ourcx/kuiklystock/ui/component/ChatComposer.kt`

## Phase 5: Polish & Cross-Cutting Concerns

- [x] T016 更新 WorkBuddy 代理配置与安全说明 `README.md`
- [x] T017 清理残留硬编码旧视觉值并完成规格追踪 `flux/changes/20260823-151315_redesign-workbuddy-ai/`

## Dependencies

`T001-T005 → US1(T006-T008) / US2(T009-T015) → T016-T017`

## Parallel Opportunities

- T003 与 T004 可并行完成。
- 基础接口稳定后，US1 的视觉组件与 US2 的 Android 网络桥接可并行。

## Implementation Strategy

先完成设计 Token 与聊天契约，再分别交付视觉重构和真实 AI 链路，最后统一清理与文档化。

<!-- cli_version: unknown -->
