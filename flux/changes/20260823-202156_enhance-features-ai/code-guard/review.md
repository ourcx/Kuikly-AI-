# 代码评审报告

- 仓库：Kuikly-AI-
- 检测模式：通用检测 + Spec-Code 一致性评审
- 检测范围：origin/main 后全部本地变更（含未跟踪代码文件）
- 生成时间：2026-08-23 21:05
- 检查文件：14
- 变更行数：1089

## 缺陷统计

- P0：0
- P1：1
- P2：1
- 合计：2

## 缺陷详情

### 1. [P1][并发问题] 并发回调可绕过去重并重复完成请求

- 位置：`shared/src/commonMain/kotlin/com/ourcx/kuiklystock/data/ResilientChatRepository.kt:30-36`
- 置信度：9/10

**问题描述**

ChatRepository 是异步回调接口，WorkBuddy 的 native callback 可能来自不同线程。这里的 completed 与 localFallbackStarted 是无同步的普通 Boolean；若两个回调同时读取 completed=false，就都能进入 complete 并分别调用最终 callback，违反类注释和规格要求的“只回调一次”。异步失败与同步异常/重复回调竞态时也可能重复启动本地降级，进而让 ChatController 重复更新同一轮状态。

**修复建议**

使用原子 compare-and-set 或互斥锁保护 completed 与 localFallbackStarted，并保证检查与更新是同一个原子操作；补充两个线程同时回调的测试。

---

### 2. [P2][规格偏离问题] 全局 MD 间距改为 12px 导致页面左右边距不符合规格

- 位置：`shared/src/commonMain/kotlin/com/ourcx/kuiklystock/theme/DesignTokens.kt:65-66`
- 置信度：10/10
- 一致性分类：错实现 / 代码层 / 已确认
- 判据来源：.reference/spec.md:L15；Testcase 来源：TC-011；生码模式：lite_tasks_driven

**问题描述**

spec.md 第 15 行明确要求页面内容统一采用 16px 左右边距、12px 卡片间距。改动把 DesignTokens.Spacing.MD 从 16f 改为 12f，而 StockHomePage、行情工具栏和列表仍用 MD 作为左右 padding，因此这些页面左右边距必然变成 12px，与验收要求直接冲突。

**修复建议**

保留 MD=16f 作为页面水平边距，新增独立的 12px 卡片间距令牌并只在卡片间距位置使用；或将页面容器显式改用 16px 令牌。

---
