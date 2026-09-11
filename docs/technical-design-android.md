# KuiklyStock Android 技术方案

## 目标与边界

首版以 Android 可运行 Demo 为目标。业务逻辑、状态和 Kuikly UI 位于 Kotlin Multiplatform `shared`；Android 宿主负责渲染容器、HTTP 请求和运行时配置。不实现交易、账号、专业 K 线或生产后台。

## 分层

- `domain`：行情、洞察、聊天、加载状态和页面状态。
- `data`：Repository 契约、腾讯/OpenAI Remote、确定性 Fixture 和降级组合。
- `presentation`：加载代次、筛选排序、详情导航、会话发送与重试。
- `ui/component`：无业务请求的 Kuikly 声明式组件。
- `androidApp`：HTTP、GB18030 解码、应用私有配置和主线程回调。

## 行情链路

`ResilientStockRepository` 先调用 `TencentStockRepository`。远端失败、超时或同步抛错时调用 `InMemoryStockRepository`。每条 `StockQuote` 携带 `QuoteDataSource`，防止 UI 把离线数据冒充实时行情。

`MarketController` 统一建模 Loading、Content、Empty、Error；加载代次阻止旧请求覆盖新状态。搜索、市场筛选、排序和自选均从稳定快照派生，不修改原始列表。

## AI 链路

`OpenAiChatRepository` 生成 OpenAI Chat Completions 兼容请求。Android 原生层校验 Base URL，自动补全 `/v1/chat/completions`，注入 Bearer Token，限制响应长度并脱敏错误。

`ResilientChatRepository` 与 `ResilientInsightRepository` 在远端未配置或失败时使用 Fixture，保证离线闭环。在线结果标记为 OpenAI，离线结果标记为本地演示。

## 页面与图表

`StockHomePage` 在行情、研究、统一详情之间切换。详情路由记录来源 Tab；从聊天卡片进入详情后，返回仍回到研究页并保留会话。

走势图使用 Kuikly Canvas 绘制折线、参考线和最新价节点。腾讯快照没有真实分时序列，因此 UI 明确称为“价格区间轨迹”，并展示振幅、区间位置和较昨收三个指标。无有效点时显示占位。

## 安全

- 不在源码或 BuildConfig 中写入真实 Token。
- 运行时 Token 只保存在应用私有配置，不回显、不记录。
- 公网地址强制 HTTPS；HTTP 仅允许 localhost、`.local` 和 RFC1918 私网地址。
- AI 分析展示投资建议免责声明，Demo 不提供交易入口。

生产化时应把 Token 转移到服务端网关或 Android Keystore，并补充鉴权、限流、审计与证书策略。

## 验证

- Repository：解析、Remote/Fixture 降级、重复回调保护。
- Domain：格式化、趋势归一化与指标计算。
- Controller：加载代次、状态切换、搜索筛选、收藏、详情、聊天发送与重试。
- Android：Debug 构建、APK 安装、冷启动、页面交互和 Logcat 检查。

执行命令见根目录 README。
