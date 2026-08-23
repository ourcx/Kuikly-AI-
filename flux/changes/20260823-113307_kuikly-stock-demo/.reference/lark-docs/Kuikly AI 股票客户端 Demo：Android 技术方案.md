# Kuikly AI 股票客户端 Demo：Android 技术方案

来源为项目绑定的飞书技术方案。本地保存实施摘要，原文仍以飞书文档为准。

- 从 Kuikly 官方模板建立 Kotlin Multiplatform 工程，首版只启用 Android 交付。
- 页面、状态和数据放在 commonMain，Android Activity 仅负责 Kuikly Render 宿主与路由桥接。
- 使用 Kuikly DSL；普通消息使用 KuiklyMarkdown，结构化卡片和走势图作为独立内容块。
- 数据层由 Repository 抽象，Fixture 是正式演示数据源，Remote 为可替换实现。
- AI 尾部元数据从 Markdown 移除后解析；解析失败只降级结构化块，不影响正文。
- 测试覆盖映射、格式化、趋势归一化、元数据解析、Fixture、聊天状态和防重。
