# Proposal: Kuikly AI 股票客户端 Demo

**Feature Directory**: `20260823-113307_kuikly-stock-demo`
**Created**: 2026-08-23
**Status**: Done
**Implemented**: Yes
**Input**: 根据项目的需求文档和技术方案实现 Kuikly AI 股票客户端 Demo，必须使用 Kuikly UI。

- 需求来源：`lark-docs/Kuikly AI 股票客户端 Demo 需求文档.md`
- 技术来源：`lark-docs/Kuikly AI 股票客户端 Demo：Android 技术方案.md`

## Clarification

### User Alignment

- 用户已明确要求依据既有需求与技术方案直接开工，并指定使用 Kuikly UI。

### Assumptions

- **首版平台**：只交付 Android，避免其他平台工具链影响首版闭环。
- **数据模式**：默认使用稳定 Fixture，远程行情和真实 AI 仅保留扩展接口。
- **技术基线**：使用官方生成器当前支持的 Kuikly DSL 2.16.0、Kotlin 2.1.21。
- **页面导航**：首页使用“行情/AI”双入口，详情页复用统一股票模型。
- **AI 内容**：Markdown 与结构化股票卡片、趋势图分离；元数据失败时降级为纯 Markdown。
- **环境验证**：Worker 使用 JDK 17 与临时 Android SDK API 34 完成共享单测和 Android Debug APK 构建。

## Design

### Context

目标仓库为空，需要从官方 Kuikly DSL 模板建立完整 KMP/Android 工程。产品需要同时覆盖行情浏览与 AI 投研两条闭环，并具备加载、空态、失败、重试等可演示状态。

### Goals / Non-Goals

**Goals:**

- 创建可由 Android 宿主加载的 Kuikly 页面。
- 完成行情列表、个股详情、AI 解读、AI 对话、Markdown、股票卡片与迷你走势图。
- 通过 Fixture 稳定演示成功、空态、失败和重试。
- 保持 presentation/domain/data 分层与可测试业务逻辑。

**Non-Goals:**

- 真实交易、账户、支付与投资建议。
- 首版 iOS/HarmonyOS 交付。
- 生产级行情采集与模型服务。
- 专业 K 线、盘口与复杂技术指标。

### Risks / Trade-offs

- Kuikly 与 Kotlin/Gradle 版本敏感，采用官方生成器输出并锁定版本。
- Markdown 组件可能增加构建依赖，使用官方推荐版本并保留纯文本降级。
- Android 构建依赖 JDK 17 与 Android SDK API 34；本次已在满足该条件的 Worker 环境完成验证。

### Impact

新建 Android 宿主、KMP shared 模块、Kuikly 页面与组件、领域模型、Fixture Repository、状态控制器、通用测试和项目文档。

## Complexity Analysis

| Dimension | Score | Rationale |
|-----------|-------|-----------|
| Scope | 3 | 空仓库从零创建，跨 Android 宿主、共享业务层、页面与测试，文件数超过 6 |
| Uncertainty | 3 | 需要确定导航、状态管理、Markdown 与结构化卡片组合方式 |
| Dependencies | 3 | 引入 Kuikly UI、KuiklyMarkdown、协程与序列化依赖 |
| Risk | 3 | 新建完整核心路径且当前无既有测试与本地 Android 构建环境 |
| Novelty | 3 | 仓库无可复用实现，属于全新 Kuikly 股票应用 |

**Total Score: 15 / 15**
**Pipeline: Thorough Design**

## Capabilities

### New Capabilities

- `market-browsing`: 浏览 Fixture 行情并进入统一详情页。
- `stock-insight`: 展示指标、趋势和 AI 风险解读。
- `ai-research-chat`: 输入问题并展示 Markdown 与结构化行情块。
- `demo-state-control`: 可重复切换加载、空态、失败并重试。

### Modified Capabilities

- 无，仓库当前为空。

### Unchanged Capabilities

- 远程 API、真实交易与多端宿主不在本期范围。
