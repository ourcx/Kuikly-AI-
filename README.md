# KuiklyStock

KuiklyStock 是使用 Kuikly UI DSL 和 Kotlin Multiplatform 构建的 Android 股票行情与 AI 研究 Demo。应用默认优先加载腾讯实时行情；网络不可用时自动回退到内置 Fixture，因此不配置任何 Token 也能完整演示行情、详情、AI 对话、Markdown、股票卡片和走势图闭环。

## 功能

- 内置 18 只 A 股、港股、美股行情，并支持输入代码从腾讯行情校验、加入和持久化自定义股票；每页最多加载 10 只。
- 行情搜索、市场筛选、涨跌/振幅排序、自选和最近浏览。
- 腾讯实时行情与离线 Fixture 自动降级，并在界面明确标注数据来源。
- 统一股票详情页：基础行情、Kuikly Canvas 趋势图、振幅/区间位置、AI 趋势与风险解读。
- AI 研究会话：SSE 流式生成、完整消息记录、建议问题、失败态、原位重试和清空会话。
- KuiklyMarkdown 渲染 Markdown，并在分析段落内嵌可点击的行情趋势卡；卡片可进入统一详情页并返回原会话。
- 可在应用内配置 OpenAI 兼容服务的 Base URL、API Token 和 Model。
- 行情页根据实际来源显示“腾讯实时行情”或“离线演示数据”；无匹配搜索可演示空态。

## 环境要求

- JDK 17
- Android SDK Platform 34 和 Build-Tools 34
- Android API 21 及以上的模拟器或真机

首次构建需要访问 Gradle 和 Maven 依赖仓库。

## 构建、测试与运行

在仓库根目录执行：

```bash
./gradlew :shared:testDebugUnitTest --no-daemon --max-workers=1
./gradlew :androidApp:assembleDebug --no-daemon --max-workers=1
```

APK 位于 `androidApp/build/outputs/apk/debug/androidApp-debug.apk`。安装并启动：

```bash
adb -s emulator-5554 install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb -s emulator-5554 shell am start -W \
  -n com.ourcx.kuiklystock/.KuiklyRenderActivity
```

设备编号以 `adb devices` 为准。不需要网络或配置即可使用 Fixture 演示核心链路。

## OpenAI 兼容服务配置

进入“研究”Tab，点击“服务连接”，填写：

- Base URL：例如 `https://api.openai.com/v1`，也可填写完整的 `/v1/chat/completions` 地址。
- API Token：通过 `Authorization: Bearer <token>` 发送，已保存的值不会回显。
- Model：由兼容服务支持的模型名称。

客户端使用 OpenAI Chat Completions 兼容请求：

```json
{
  "model": "your-model",
  "messages": [
    {"role": "system", "content": "分析边界与格式要求"},
    {"role": "user", "content": "用户问题与行情上下文"}
  ],
  "stream": true
}
```

Base URL 规则：

- 公网地址必须使用 HTTPS。
- localhost、`.local` 和私网 IP 可使用 HTTP，便于本地网关联调。
- 只填写到 `/v1` 时，客户端会自动补全 `/chat/completions`。
- Token 保存在 Android 应用私有配置中，不会写入仓库或日志。正式生产环境建议改用服务端代理或系统级安全存储。

也可用构建变量提供非敏感默认地址与模型：

```bash
OPENAI_PROXY_URL="https://gateway.example.com/v1" \
OPENAI_MODEL="your-model" \
./gradlew :androidApp:assembleDebug --no-daemon --max-workers=1
```

禁止把真实 Token、Cookie 或个人凭证写入源码、构建脚本、`local.properties` 或提交记录。

## 架构与目录

```text
androidApp/
└── src/main/                         # Android 宿主、原生网络桥接和资源
shared/src/commonMain/kotlin/com/ourcx/kuiklystock/
├── data/                             # Remote、Fixture 与降级 Repository
├── domain/                           # 行情、详情、聊天与页面状态
├── presentation/                     # Market / Chat / Home Controller
├── ui/component/                     # Kuikly 页面与复用组件
├── theme/                            # DesignTokens
├── base/                             # Kuikly Native Bridge
└── StockHomePage.kt                  # 应用入口与路由
shared/src/commonTest/                # 共享业务单元测试
docs/                                 # 需求、技术方案和演示视频
```

```text
TencentStockRepository ─┐
                        ├─ ResilientStockRepository → MarketController → Kuikly UI
InMemoryStockRepository ┘

OpenAiChatRepository ───┐
                        ├─ ResilientChatRepository → ChatController → Markdown / Cards
InMemoryChatRepository ─┘
```

共享 Kotlin 层负责业务状态与 UI，Android 宿主只负责网络、持久化和 Kuikly 渲染桥接。

## 相关文档

- [需求与验收映射](docs/requirements.md)
- [Android 技术方案](docs/technical-design-android.md)
- [交付清单与完成判断](docs/delivery-checklist.md)
- [演示视频说明](docs/demo/README.md)

## 项目亮点与限制

- Remote-first、Fixture fallback，既能展示真实能力，也能稳定离线验收。
- 同一详情页承接行情入口和聊天卡片入口，返回时保留原 Tab 与会话。
- Kuikly Canvas 轻量绘图，不引入大型图表框架。
- OpenAI Chat Completions 兼容配置运行时生效，错误正文和鉴权字段会脱敏。
- 当前只验证 Android；iOS / OpenHarmony 目录不代表对应平台已经完成。
- 腾讯快照没有完整分时序列，图表展示价格区间轨迹，不冒充专业 K 线。
- 最近浏览和会话只在当前进程内保存；自定义股票代码会持久化到 Android 应用私有配置。
- Demo 不包含真实交易、下单或收益承诺。
