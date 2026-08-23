# KuiklyStock

KuiklyStock 是使用 Kuikly UI DSL 构建的股票行情与 AI 投研演示应用。项目采用 Kotlin Multiplatform 组织共享代码，当前实际启用并验证的运行目标为 Android；行情与 AI 回复均来自本地 Fixture，不依赖线上服务。

## 功能清单

- 行情与 AI 投研双 Tab 首页。
- 自选行情列表，展示股票名称、代码、最新价、涨跌额和涨跌幅。
- 统一股票详情页，展示价格、高低价、成交量、趋势与 AI 风险解读。
- AI 投研会话，支持问题输入、发送、生成状态、失败提示与重试。
- AI 回复支持 Markdown、股票卡片和迷你走势图，并可从股票卡片进入统一详情页。
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

Android 宿主负责应用启动、Kuikly 渲染容器和页面路由，业务状态、领域模型与 Kuikly UI 位于 `shared`。`StockHome` 是演示应用的单页面入口，页面内部通过状态切换行情、AI 投研和统一股票详情。

```text
androidApp/
└── src/main/                  # Android Application、渲染 Activity 与资源
shared/
└── src/
    ├── commonMain/kotlin/com/ourcx/kuiklystock/
    │   ├── data/              # Repository 契约实现与离线 Fixture
    │   ├── domain/            # 行情、详情、会话模型及页面状态
    │   ├── presentation/      # 行情、AI 会话和首页控制器
    │   ├── ui/component/      # Kuikly 页面内容与可复用组件
    │   ├── theme/             # DesignTokens 视觉常量
    │   ├── base/              # Kuikly 页面基础设施与桥接能力
    │   └── StockHomePage.kt   # StockHome 单页面入口
    └── commonTest/            # 共享业务逻辑与控制器测试
```

主要数据流为：

```text
Fixture Repository → Controller → Page State → Kuikly UI
```

`data` 层可在后续替换为真实行情或 AI 服务实现，`domain`、`presentation` 与 UI 状态结构无需依赖具体数据来源。

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

构建成功后，Debug APK 位于 `androidApp/build/outputs/apk/debug/`。本次交付已在 JDK 17、Android SDK API 34 与 Build-Tools 34 环境完成共享单测及 Debug APK 构建；若其他环境构建失败，请先核对这些工具链版本和依赖仓库访问状态。

## Fixture 演示

项目默认使用内存 Repository 提供确定性的离线数据，启动后无需配置网络接口或凭证：

- 行情 Tab 默认展示多市场股票 Fixture，可进入任意股票详情。
- 行情页顶部的演示入口可切换“内容”“空数据”和“错误”状态；错误状态支持重试。加载状态由控制器统一建模。
- AI 投研会根据问题中出现的股票代码或名称返回对应的 Markdown 解读、股票卡片和趋势数据。未匹配股票时使用默认演示标的。
- 在 AI 问题中包含“失败”可触发确定性的失败流程，并验证重试交互。
- 所有行情、观点与时间均为演示数据，不构成投资建议。

## DesignTokens 规范

页面颜色、字号、间距、圆角与通用尺寸统一由 `shared/src/commonMain/kotlin/com/ourcx/kuiklystock/theme/DesignTokens.kt` 管理。视觉基调为深色蓝黑背景、slate 卡片、蓝色强调色，以及红涨绿跌的行情语义。

新增或调整 UI 时应优先复用现有 Token，避免在页面和组件中散落视觉常量。当前需求没有提供精确的颜色十六进制值；后续若设计规范更新，应在 `DesignTokens.kt` 中单点替换并统一生效。

## Markdown 样式说明

AI 会话中的 Markdown 内容使用 `KuiklyMarkdown` 1.0.6-2.1.21，并通过应用级 `MarkdownConfig` 将正文、标题、引用、链接和代码块映射到 `DesignTokens`。

## 当前限制

- 当前仅启用 Android 构建目标；仓库中的 iOS、OpenHarmony 等目录不代表本 Demo 已完成对应平台适配。
- 行情和 AI 能力均为本地 Fixture，不包含实时行情、真实模型调用、账号体系、交易能力或生产级数据持久化。
- AI 回复中的结构化股票元数据用于演示；元数据不可用时应保留 Markdown 正文作为降级展示。
