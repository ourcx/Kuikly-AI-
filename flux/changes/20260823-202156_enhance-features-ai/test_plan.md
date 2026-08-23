# Test Plan

## Overview

- **用户意图**：验证股票客户端功能增强、AI 可用性与 DESIGN.md 视觉对齐优化。
- **测试范围**：测试所有本地 diff，与 origin/main 对比。
- **需求参考列表**：来源：变更规格与用户输入；摘要：Kuikly UI 行情发现、自选、一键 AI、WorkBuddy 优先与本地降级、深靛蓝霓虹视觉和统一栅格。
- **变更类型**：新功能与界面优化
- **工作流**：SDD + SDT（复用已有目录）
- **关联开发目录**：flux/changes/20260823-202156_enhance-features-ai
- **已选测试能力**：bits-code-guard、bits-unit-test-gen、flux-gui-test

## Unit Tests (bits-unit-test-gen)

将在实现阶段验证行情搜索、市场筛选、排序、自选组合，AI 远端优先与本地降级、单次回调、清空会话及一键个股分析。

## GUI Tests (flux-gui-test)

将在实现阶段执行移动端 GUI 快速验收，覆盖行情发现、自选、空态恢复、一键 AI、来源标识、会话交互与 DESIGN.md 暗色排版对齐。

## Code Guard (bits-code-guard)

将在实现阶段检查 origin/main 之后全部本地变更的逻辑、安全、并发、健壮性、性能与代码规范。

<!-- cli_version: 0.2.14 -->
