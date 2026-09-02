# KuiklyStock

KuiklyStock 是使用 Kuikly UI DSL 构建的股票行情与研究演示应用。项目采用 Kotlin Multiplatform 组织共享代码，当前实际启用并验证的运行目标为 Android；行情数据来自本地 Fixture，研究会话可通过合作方 HTTPS 代理接入 WorkBuddy OpenAPI。

## 功能清单

- 行情与研究双 Tab 首页。
- 行情发现支持按名称、代码、交易所搜索，按港股、A 股、美股筛选，并支持涨幅、跌幅和日内振幅排序。
- 会话级自选列表，支持收藏、仅看自选，以及空结果一键恢复筛选。
- 会话级最近浏览记录按最新优先去重，支持快速返回最近研究的 3 个标的。
- 紧凑行情列表展示统一对齐的名称、代码、最新价、涨跌额、涨跌幅与日内高低，并支持进入详情或发起研究。
- 统一股票详情页，展示价格、高低价、成交量、趋势与风险提示。
- 研究会话支持问题输入、常用任务、一键个股研究、发送、清空、处理状态、失败提示与重试。
- 在线服务采用 WorkBuddy 优先、本地行情分析兜底的双通道策略，并在界面明确展示实际来源。
- 在线回复支持 Markdown、股票卡片和迷你走势图，并可从股票卡片进入统一详情页。
- 服务连接支持在应用内配置 HTTPS 代理地址，保存后无需重建或重启即可使用。
- 行情流程支持内容、加载、空数据、失败及重试等演示状态。

## 技术栈

| 项目 | 版本或配置 |
| --- | --- |
| Kotlin | 2.1.21 |
| Kotlin Multiplatform | 当前启用 Android target |
| Kuikly UI DSL | 2.16.0 |
| KuiklyMarkdown | 1.0.6-2.1.21 |
| Gradle Wrapper | 8.5 |
| Android SDK | compileSdk 34 / minSdk 21 / targetSdk 30 |

## 技术架构

Android 宿主负责应用启动、Kuikly 渲染容器和页面路由，业务状态、领域模型与 Kuikly UI 位于 `shared`。`StockHome` 是演示应用的单页面入口，页面内部通过状态切换行情、研究和统一股票详情。

```text
androidApp/
└── src/main/                  # Android Application、渲染 Activity 与资源
shared/
└── src/
    ├── commonMain/kotlin/com/ourcx/kuiklystock/
    │   ├── data/              # Repository 契约实现与离线 Fixture
    │   ├── domain/            # 行情、详情、会话模型及页面状态
    │   ├── presentation/      # 行情、研究会话和首页控制器
    │   ├── ui/component/      # Kuikly 页面内容与可复用组件
    │   ├── theme/             # DesignTokens 视觉常量
    │   ├── base/              # Kuikly 页面基础设施与桥接能力
    │   └── StockHomePage.kt   # StockHome 单页面入口
    └── commonTest/            # 共享业务逻辑与控制器测试
```

行情数据流为：

```text
Fixture Repository → Controller → Page State → Kuikly UI
```

研究会话数据流为：

```text
Kuikly UI DSL → ChatController → ResilientChatRepository
    ├── 已配置且可用 → WorkBuddyChatRepository → BridgeModule
    │   → KRBridgeModule → 合作方 HTTPS 代理 → WorkBuddy OpenAPI
    └── 未配置或失败 → InMemoryChatRepository → 本地行情分析
```

`shared` 中的 Kuikly UI DSL 和控制器只依赖 Repository 与 Bridge 契约；Android 宿主负责 HTTPS 网络请求。行情 `data` 层仍可在后续替换为真实服务实现，而无需改动页面状态结构。

## 本地运行环境

准备以下环境后，在仓库根目录执行构建命令：

- JDK 17。
- Android SDK Platform 34。
- Android SDK Build-Tools 34。
- 可用的 Android 模拟器或 API 21 及以上真机。
- 首次构建需要能够下载 Gradle 插件及 Maven 依赖。

请确保本机 Android SDK 路径已通过 Android Studio 或 `local.properties` 正确配置。

## 构建与测试

运行共享模块单元测试：

```bash
./gradlew :shared:testDebugUnitTest --no-daemon --max-workers=1
```

构建 Android Debug APK：

```bash
./gradlew :androidApp:assembleDebug --no-daemon --max-workers=1
```

需要连接 WorkBuddy 时，可在应用“研究”页打开“服务连接”，填写合作方 HTTPS 代理地址。地址保存于应用私有存储，下一次发送立即生效。也可通过构建进程环境变量提供默认地址：

```bash
WORKBUDDY_PROXY_URL="https://proxy.example.com/workbuddy/chat" ./gradlew :androidApp:assembleDebug --no-daemon --max-workers=1
```

运行时地址优先于 `BuildConfig` 中的 `WORKBUDDY_PROXY_URL` 默认值。客户端仅接受协议为 `https`、包含有效主机名且不含 userinfo、查询参数或 fragment 的地址。地址未设置、代理超时、非 2xx 或响应无效时，应用会自动切换为本地行情分析；界面会明确展示当前来源，不会把本地结果标记为在线结果。

`local.properties` 只用于 Android SDK 等本机工具链路径；不建议用它保存合作方地址，更不得写入凭证或其他敏感内容。构建示例中的域名仅为占位符。

构建成功后，Debug APK 位于 `androidApp/build/outputs/apk/debug/`。本次交付已在 JDK 17、Android SDK API 34 与 Build-Tools 34 环境完成共享单测及 Debug APK 构建；若其他环境构建失败，请先核对这些工具链版本和依赖仓库访问状态。

## WorkBuddy 代理接口契约

Android 客户端向 `WORKBUDDY_PROXY_URL` 指定的合作方接口发送 `POST` 请求，请求与响应的 `Content-Type` 均为 `application/json`。合作方后端负责认证 WorkBuddy OpenAPI、转发请求，并将响应整理为以下客户端契约。

请求体：

```json
{
  "question": "请分析示例标的近期走势",
  "conversation_id": "example-conversation-id",
  "context": {
    "quotes": [
      {
        "symbol": "DEMO",
        "name": "示例标的",
        "exchange": "EXAMPLE",
        "price": 100.0,
        "change": 1.2,
        "change_percent": 1.21
      }
    ]
  }
}
```

- `question`：必填，用户问题。
- `conversation_id`：可选，上一轮响应返回的会话标识，用于续接上下文。
- `context.quotes`：当前本地行情摘要数组，元素包含 `symbol`、`name`、`exchange`、`price`、`change` 和 `change_percent`。

响应体：

```json
{
  "answer": "示例 Markdown 回答",
  "conversation_id": "example-conversation-id",
  "symbols": ["DEMO"],
  "show_trend": true
}
```

- `answer`：必填，供 KuiklyMarkdown 渲染的回答正文。
- `conversation_id`：可选，客户端保存后用于下一轮请求。
- `symbols`：可选，关联股票代码列表；缺省时按空列表处理。
- `show_trend`：可选，是否展示关联标的趋势；缺省时为 `false`。

合作方接口应返回 HTTP 2xx 和符合上述契约的 JSON。超时、非 2xx、网络错误或无效 JSON 会在客户端转换为可读的失败状态，用户可以对原问题重试。

## WorkBuddy 安全边界

- Android 客户端只连接应用内或构建时配置的合作方 HTTPS 代理，不直接调用 WorkBuddy OpenAPI，也不接受 HTTP 明文地址。
- 应用内仅保存代理 URL，不提供 Token、Cookie、密码、私钥或 OAuth 票据输入项。
- WorkBuddy 凭证、`access_token`、`refresh_token`、`client_secret`，以及 OAuth PKCE verifier、authorization code 等票据只能由合作方后端持有和处理，禁止写入客户端、构建变量、`local.properties`、源码或版本库。
- 合作方后端负责凭证安全存储、OAuth 流程、令牌刷新、访问控制、限流、审计和上游错误收敛；不得把上游凭证透传给客户端。
- 客户端请求只携带接口契约所列业务字段，不应把凭证、个人信息或其他敏感数据放入 `question`、`conversation_id` 或 `context.quotes`。
- 客户端不记录请求正文或响应正文；展示给用户的网络错误会限制长度并脱敏常见令牌和密钥字段。

## Fixture 演示

项目默认使用内存 Repository 提供确定性的离线行情数据；行情功能启动后无需配置网络接口或凭证：

- 行情 Tab 默认展示多市场股票 Fixture，可进入任意股票详情。
- 行情页由控制器统一建模加载、内容、空数据和错误状态；错误状态支持重试。
- 研究功能无需配置即可使用本地行情分析；配置合作方 HTTPS 代理后优先使用 WorkBuddy，调用失败时自动降级。
- 所有行情、观点与时间均为演示数据，不构成投资建议。

## DesignTokens 规范

页面颜色、字号、间距、圆角与通用尺寸统一由 `shared/src/commonMain/kotlin/com/ourcx/kuiklystock/theme/DesignTokens.kt` 管理。视觉严格采用 `DESIGN.md` 的深靛蓝夜空背景、深色悬浮卡片、霓虹粉主操作、霓虹青辅助高亮，以及红涨绿跌的行情语义；页面壳层、内容卡片和底部导航共用 16px 水平栅格。

新增或调整 UI 时应优先复用现有 Token，避免在页面和组件中散落视觉常量。当前需求没有提供精确的颜色十六进制值；后续若设计规范更新，应在 `DesignTokens.kt` 中单点替换并统一生效。

## Markdown 样式说明

研究会话中的 Markdown 内容使用 `KuiklyMarkdown` 1.0.6-2.1.21，并通过应用级 `MarkdownConfig` 将正文、标题、引用、链接和代码块映射到 `DesignTokens`。

## 当前限制

- 当前仅启用 Android 构建目标；仓库中的 iOS、OpenHarmony 等目录不代表本 Demo 已完成对应平台适配。
- 行情能力仍为本地 Fixture；未配置 WorkBuddy 时研究页使用这些行情生成确定性本地分析。本项目不包含实时行情、账号体系、交易能力或生产级数据持久化，自选与最近浏览仅保留在当前应用会话。
- 在线回复中的结构化股票元数据用于演示；元数据不可用时应保留 Markdown 正文作为降级展示。
