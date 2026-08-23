# 单元测试生成结果汇总
**生成状态**：成功；**生成耗时**：`5.5 分钟`
---
## 总体统计
**单测增量覆盖率**：`-`；**命中函数数**：18；**生成用例数**：8；**执行通过用例数**：8；
**修复编译失败包**：0；**修复执行失败用例数**：0；**发现缺陷数**：0
---
## 文件明细
| 文件路径 | 通过用例数 | 总用例数 | 覆盖率 |
|:---------|:----------:|:--------:|:------:|
| [shared/src/commonMain/kotlin/com/ourcx/kuiklystock/presentation/MarketController.kt](file:////ws/.flux/worker/86849345793569084/repos/ourcx/Kuikly-AI-/shared/src/commonMain/kotlin/com/ourcx/kuiklystock/presentation/MarketController.kt) | 3 | 3 | - |
| [shared/src/commonMain/kotlin/com/ourcx/kuiklystock/presentation/ChatController.kt](file:////ws/.flux/worker/86849345793569084/repos/ourcx/Kuikly-AI-/shared/src/commonMain/kotlin/com/ourcx/kuiklystock/presentation/ChatController.kt) | 2 | 2 | - |
| [shared/src/commonMain/kotlin/com/ourcx/kuiklystock/presentation/StockHomeController.kt](file:////ws/.flux/worker/86849345793569084/repos/ourcx/Kuikly-AI-/shared/src/commonMain/kotlin/com/ourcx/kuiklystock/presentation/StockHomeController.kt) | 1 | 1 | - |
| [shared/src/commonMain/kotlin/com/ourcx/kuiklystock/data/ResilientChatRepository.kt](file:////ws/.flux/worker/86849345793569084/repos/ourcx/Kuikly-AI-/shared/src/commonMain/kotlin/com/ourcx/kuiklystock/data/ResilientChatRepository.kt) | 2 | 2 | - |
---
## 生成明细
| 文件名 | 执行成功数/生成用例数 | 生成后增量覆盖率 |
|:-------|:--------------------:|:----------------:|
| [MarketControllerTest.kt](file:////ws/.flux/worker/86849345793569084/repos/ourcx/Kuikly-AI-/shared/src/commonTest/kotlin/com/ourcx/kuiklystock/presentation/MarketControllerTest.kt) | 3/3 | - |
| [ChatAndHomeControllerTest.kt](file:////ws/.flux/worker/86849345793569084/repos/ourcx/Kuikly-AI-/shared/src/commonTest/kotlin/com/ourcx/kuiklystock/presentation/ChatAndHomeControllerTest.kt) | 3/3 | - |
| [ResilientChatRepositoryTest.kt](file:////ws/.flux/worker/86849345793569084/repos/ourcx/Kuikly-AI-/shared/src/commonTest/kotlin/com/ourcx/kuiklystock/data/ResilientChatRepositoryTest.kt) | 2/2 | - |
---
## 用例修复明细
| 修复类型 | 修复对象| 包含用例数 | 修复后增量覆盖率 |
|:-------|:-------:|:---------:|:-------------:|
| 环境补齐 | [shared](file:////ws/.flux/worker/86849345793569084/repos/ourcx/Kuikly-AI-/shared) | 8 | - |
---
## 缺陷明细
暂无
---
## 失败用例明细
暂无。补齐 Android SDK 后，`testDebugUnitTest` 共执行 38 条用例，38 条全部通过。
---
## 跳过函数明细
暂无
---
## 补测建议
建议后续在 CI 中持续执行 Android 单测并接入覆盖率采集；本轮未配置覆盖率插件。
