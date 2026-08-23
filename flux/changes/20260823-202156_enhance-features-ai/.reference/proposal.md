# 股票客户端功能与 AI 可用性增强提案

**Feature Directory**: `20260823-202156_enhance-features-ai`
**Created**: 2026-08-23
**Status**: Done
**Implemented**: Yes
**Input**: 用户反馈当前功能单一、AI 无法使用，要求增加功能、优化 AI 可用性，并修复部分排版未对齐问题。视觉继续以 `artifacts/specs/design/DESIGN.md` 为唯一基准，业务 UI 继续使用 Kuikly UI DSL。

---

## Clarification

### User Alignment

- 用户要求增强产品功能，而不是只做视觉微调。
- 用户要求 AI 在默认安装下可使用，同时保留 WorkBuddy 真实接入能力。
- 用户要求修复页面栅格、间距和信息对齐。

### Assumptions

- **AI 双通道**：配置合法 `WORKBUDDY_PROXY_URL` 时优先调用真实 WorkBuddy；未配置或代理请求失败时使用本地行情智能分析降级，且 UI 明确标识“本地分析”，不伪装成在线模型。
- **行情交互范围**：新增名称/代码搜索、全部/港股/A 股/美股筛选、涨跌排序与自选收藏，均在本地确定性行情快照上工作。
- **快速 AI 入口**：行情卡片提供“一键问 AI”，自动切换到 AI Tab 并带入对应标的问题，减少操作路径。
- **状态持久化边界**：本轮自选只保持当前应用会话内状态，不新增数据库或账号同步。
- **网络安全边界不变**：WorkBuddy OAuth、Token、Cookie 与 client secret 继续只保存在合作方后端，客户端不直接连接 WorkBuddy OpenAPI。
- **布局统一**：页面统一使用 16px 内容边距、12px 卡片间距、固定右侧价格栏和一致的卡片内部对齐，不引入 Compose 或 Android View 业务 UI。

---

## Complexity Analysis

| Dimension | Score | Rationale |
|---|---:|---|
| Scope | 3 | 跨领域模型、Controller、Repository、Kuikly UI 与测试，涉及 6 个以上文件 |
| Uncertainty | 2 | 功能方向明确，但 AI 无配置时的可用策略和新增功能组合需产品默认决策 |
| Dependencies | 2 | 复用现有 WorkBuddy 代理与 Bridge，但需新增仓库组合和状态契约 |
| Risk | 3 | 修改行情与聊天核心交互路径，需防止状态和异步回调回归 |
| Novelty | 2 | 现有分层与组件可复用，但筛选/自选/排序和 AI 降级是新增能力 |

**Total Score: 12 / 15**
**Pipeline: Thorough Design**

---

## Design

### Context

当前版本视觉已符合 DESIGN.md，真实 WorkBuddy 通过可选 HTTPS 代理接入，但默认未配置环境下 AI 直接失败；行情页只有静态卡片与详情跳转，分类 Chip 也没有交互，导致用户感知功能单一。部分组件的左右边距、价格列与按钮位置缺少统一栅格。

### Goals / Non-Goals

**Goals:**

- 让默认安装在没有代理配置时仍能产生基于本地行情的结构化 AI 回答。
- 配置 WorkBuddy 时保持真实在线 AI 为首选，失败时无缝降级并提示来源。
- 增加行情搜索、市场筛选、自选、排序和一键 AI 分析。
- 对齐页面标题、筛选区、卡片内容、价格列、输入区和底部导航。
- 为新增状态与仓库策略补充单元测试，并通过 Android Debug 构建。

**Non-Goals:**

- 不新增账号、交易、实时行情、数据库或跨设备自选同步。
- 不在客户端保存 WorkBuddy 凭证，也不绕过合作方代理。
- 不引入 Compose、原生 Android 业务 View 或新的第三方网络库。

### Risks / Trade-offs

- 本地分析基于确定性行情与规则，不等同于在线大模型；通过来源 Badge 和说明避免误导。
- 会话内自选实现简单可靠，但应用重启后不会保留。
- 小屏横向筛选项较多，采用紧凑 Chip 与分组控制避免溢出。

### Impact

- 扩展 `MarketState`、`ChatState` 及 Controller 事件。
- 新增可回退聊天 Repository，复用现有 WorkBuddy 与 InMemory 实现。
- 重构 `MarketContent`、`AiResearchContent`、`StockHomePage` 的回调与栅格。
- 更新测试与 README，并提交推送 GitHub `main`。

## Capabilities

### New Capabilities

- `market-discovery-controls`：搜索、市场筛选、涨跌排序与结果计数。
- `session-watchlist`：会话内自选收藏和仅看自选。
- `quick-ai-analysis`：从行情卡片直接生成标的分析问题并切换 AI 页面。
- `resilient-ai`：WorkBuddy 优先、本地行情分析降级，默认即可使用。

### Modified Capabilities

- `market-gallery`：从静态卡片列表升级为可交互发现工具，并统一栅格对齐。
- `ai-research`：展示在线/本地分析来源，支持清空会话和建议问题。
- `app-shell`：修复页头、内容区、底栏和卡片的水平对齐。

### Unchanged Capabilities

- `workbuddy-security-boundary`：仍由合作方 HTTPS 代理持有 OAuth 与票据。
- `stock-detail`：保留详情、指标、趋势、洞察与免责声明。
- `kuikly-ui`：所有业务 UI 继续使用 Kuikly UI DSL。
