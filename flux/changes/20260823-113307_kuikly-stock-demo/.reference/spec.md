# Feature Specification: Kuikly AI 股票客户端 Demo

**Feature Spec**: `20260823-113307_kuikly-stock-demo`
**Base Commit**: `HEAD`
**Created**: 2026-08-23
**Status**: Done
**Input**: 根据项目需求和技术方案，交付一个使用 Kuikly UI 的 Android 股票客户端 Demo。

## User Scenarios

### User Story 1 - 浏览行情并查看个股解读 (Priority: P1)

用户打开应用后可浏览自选股票，进入详情查看价格、指标、趋势及 AI 风险解读。

**Why this priority**: 这是股票客户端的基础价值和第一条完整演示链路。

**Acceptance Scenarios**:

1. **Given** 行情加载成功，**When** 用户滚动列表，**Then** 每项展示名称、代码、最新价、涨跌额和涨跌幅。
2. **Given** 用户点击任意股票，**When** 进入详情，**Then** 展示基础行情、指标、迷你趋势和 AI 解读。
3. **Given** 行情为空或失败，**When** 页面完成加载，**Then** 展示对应状态并允许失败状态重试。

### User Story 2 - 使用 AI 投研助手 (Priority: P1)

用户输入股票问题后可获得组合式回复，并从回复中的股票卡片进入详情。

**Why this priority**: 这是 AI 与股票场景结合的核心差异化链路。

**Acceptance Scenarios**:

1. **Given** 输入内容非空且当前未发送，**When** 用户发送，**Then** 会话立即追加用户消息并展示生成状态。
2. **Given** Fixture 返回完成，**When** 回复展示，**Then** 同时呈现 Markdown、股票卡片和迷你走势图。
3. **Given** 用户点击回复中的股票卡片，**When** 页面跳转，**Then** 展示对应股票的统一详情页。
4. **Given** 回复失败，**When** 用户重试，**Then** 不重复追加原用户消息并重新生成回复。

### User Story 3 - 稳定演示异常状态 (Priority: P2)

演示者可以通过本地状态切换稳定复现加载、空态和失败场景。

**Why this priority**: 验收要求必须覆盖完整状态，且演示不能依赖外部服务。

**Acceptance Scenarios**:

1. **Given** 应用使用本地演示数据，**When** 切换状态，**Then** 页面确定性展示目标状态。
2. **Given** 失败状态，**When** 点击重试，**Then** 页面回到成功数据。

### Edge Cases

- 空白问题不会发送；连续点击发送不会产生重复消息。
- 未知股票代码不会崩溃，展示可返回的缺失态。
- AI 元数据无效时仍展示 Markdown 正文。
- 趋势点全部相同时仍能生成稳定的归一化结果。
- 负涨跌、零涨跌、正涨跌使用清晰且一致的符号与颜色。

## Non-Goals

- **NG-001**: 真实交易、账户和支付——超出 Demo 范围且涉及高风险金融能力。
- **NG-002**: 生产级实时行情与 AI 服务——首版以离线稳定演示为优先。
- **NG-003**: iOS 与 HarmonyOS 交付——首版只验收 Android。
- **NG-004**: 专业 K 线、盘口和复杂指标——不属于首版验收链路。

## Requirements

### Functional Requirements

- **FR-001**: 应用必须提供行情与 AI 投研两个主要入口。
- **FR-002**: 行情列表必须展示至少五只股票及完整涨跌信息。
- **FR-003**: 用户必须能够从行情列表进入对应股票详情。
- **FR-004**: 详情必须展示最新价、涨跌、高低价、成交量、趋势和 AI 解读。
- **FR-005**: AI 投研必须支持输入、发送、会话记录与生成状态。
- **FR-006**: AI 回复必须支持 Markdown 文本、股票卡片与迷你走势图。
- **FR-007**: 用户必须能够从 AI 股票卡片进入对应详情。
- **FR-008**: 行情与 AI 流程必须提供加载、空态、失败和重试反馈。
- **FR-009**: 默认数据源必须支持无网络稳定演示。
- **FR-010**: 所有投资分析必须展示非投资建议免责声明。

### Key Entities

- **StockQuote**: 股票代码、名称、价格、涨跌、成交数据和趋势点。
- **StockInsight**: 趋势判断、摘要、风险和信号。
- **ChatMessage**: 角色、内容块、生成状态和重试关联。
- **ChatContentBlock**: Markdown、股票卡片或趋势图。
- **LoadState**: 加载、成功、空态或失败。

## Assumptions

- 首版内容为演示数据，不构成投资建议。
- 用户无需登录即可使用全部功能。
- 页面状态只需在本次应用进程内保持。

## Success Criteria

### Measurable Outcomes

- **SC-001**: 用户可在 3 次点击内完成“行情列表到个股 AI 解读”链路。
- **SC-002**: 用户可在 4 次操作内完成“输入问题到股票卡片详情”链路。
- **SC-003**: 成功、空态、失败与重试四类状态均可确定性复现。
- **SC-004**: 100% 的演示主链路在无网络条件下可用。
- **SC-005**: 无效结构化元数据不会阻断 AI 文本展示。

<!-- cli_version: unknown -->
