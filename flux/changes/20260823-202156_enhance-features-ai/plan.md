# 股票客户端功能与 AI 可用性增强技术方案

## 架构

继续沿用 `Kuikly UI → Controller → Repository → Bridge/Fixture` 分层：

- `MarketController` 持有原始行情快照和发现工具状态，统一生成过滤排序后的 `MarketState`。
- `StockHomeController` 负责跨模块动作：从行情卡片切换 AI Tab 并触发问题。
- `ResilientChatRepository` 组合 `WorkBuddyChatRepository` 与 `InMemoryChatRepository`：WorkBuddy 可用时优先，未配置或失败时回退本地分析。
- `ChatController` 继续负责会话、防重、重试和 conversation_id，并记录最近回答来源。
- Kuikly 组件只消费不可变状态并通过回调发出事件。

## 数据模型

- `MarketFilter`: `ALL`, `HK`, `CN`, `US`。
- `MarketSort`: `DEFAULT`, `GAINERS`, `LOSERS`。
- `MarketState`: 增加 query、filter、sort、favoriteSymbols、favoritesOnly、totalCount。
- `ChatProvider`: `WORKBUDDY`, `LOCAL`。
- `ChatResponse`: 增加默认 `provider`；远端未返回该字段时默认 WorkBuddy，本地仓库显式返回 Local。
- `ChatState`: 增加 provider。

## 数据流

### 行情发现

1. `load()` 保存稳定原始快照。
2. 搜索、筛选、排序、自选事件更新控制器字段。
3. `publishMarket()` 按查询 → 市场 → 自选 → 排序顺序计算结果并发布状态。
4. UI 更新结果数量、Chip 选中态和股票卡片。

### AI 双通道

1. 首页构造 WorkBuddy 与本地 Repository，再交给 `ResilientChatRepository`。
2. 若 WorkBuddy 未配置，直接调用本地分析。
3. 若 WorkBuddy 异步失败，自动调用本地分析，并只向 Controller 回调最终结果。
4. Controller 根据响应 provider 更新来源 Badge。

### 快速 AI

1. 股票卡片上报 symbol。
2. 首页控制器读取 quote，切换 AI Tab，写入“分析名称（代码）...”问题并发送。
3. AI 页面展示在线或本地结果。

## 布局策略

- 页面壳和内容组件只保留一层 16px 水平边距，避免重复 padding。
- 行情工具区分两行：搜索框；市场筛选 + 排序/自选控制。
- 股票卡片分为左侧身份区、固定宽度价格区、底部操作行。
- 所有卡片使用统一 16px 圆角、16px 内边距、12px 垂直间距。

## 测试策略

- `MarketControllerTest`: 搜索、市场筛选、排序、自选组合。
- `ChatAndHomeControllerTest`: 快速 AI、清空会话、来源状态。
- `ResilientChatRepositoryTest`: 未配置直接降级、WorkBuddy 成功、WorkBuddy 失败降级、双失败。
- 运行 `:shared:testDebugUnitTest` 与 `:androidApp:assembleDebug`，限制单 worker、1 GiB JVM。
