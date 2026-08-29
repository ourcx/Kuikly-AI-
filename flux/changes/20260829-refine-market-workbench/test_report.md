# 行情工作台与 AI 投研体验优化测试报告

## 验证结果

- `:shared:testDebugUnitTest`：39 条测试通过，0 失败。
- `:androidApp:assembleDebug`：构建成功。
- Debug APK：`androidApp/build/outputs/apk/debug/androidApp-debug.apk`。
- `git diff --check`：通过。
- 变更敏感信息扫描：未发现新增凭证、令牌、密码或私钥。

## 覆盖范围

- 默认、涨幅、跌幅、日内振幅排序。
- 最近浏览按最新优先去重并限制为 3 条。
- 无效股票不会写入最近浏览。
- 清除筛选保留最近浏览和自选状态。
- Kuikly UI 主源码与 Android 宿主完整编译。

## 已知提示

构建期间 D8 对部分 Kotlin metadata 输出兼容性警告，但未中断 dex、打包或 APK 生成；该提示为当前 Kotlin/Android Gradle Plugin 组合的既有行为。
