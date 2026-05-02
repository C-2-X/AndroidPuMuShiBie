# AndroidPuMuShiBie - 全局 OCR 悬浮球工具

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![minSdk](https://img.shields.io/badge/minSdk-29-red)
![targetSdk](https://img.shields.io/badge/targetSdk-34-blue)

---

## 目录

- [项目概述](#项目概述)
- [环境要求](#环境要求)
- [快速上手](#快速上手)
- [项目架构](#项目架构)
- [核心组件](#核心组件)
- [开发工作流](#开发工作流)
- [编码规范](#编码规范)
- [测试](#测试)
- [常见问题](#常见问题)
- [联系方式](#联系方式)

---

## 项目概述

### 定位

极简 Android 悬浮球工具：点击即触发截屏框选，通过百度 OCR API 识别屏幕标准印刷文本（中英混排），结果支持逐行/跨行多选复制，悬浮球常驻后台（含视频全屏场景）。

### 功能特点

1. **屏幕边缘悬浮球**：仅贴靠左右边缘，可拖拽移动位置，锁屏自动隐藏
2. **一键截屏+框选**：点击悬浮球 → 屏幕冻结 → 半透明蒙层 → 拖拽四角调整大小/选区整体移动
3. **百度 OCR 集成**：支持自定义 API Key/Secret Key，自动 Token 缓存，提前刷新避免过期
4. **结果面板**：可拖拽浮动卡片，高度自适应，内容多时可静默滚动（无可见滚动条）
5. **历史记录**：本地保存最近 100 条识别文本，支持单条删除和清空
6. **权限按需申请**：悬浮窗/无障碍/屏幕录制（MediaProjection）按需引导，被拒绝后提供跳转系统设置快捷方式
7. **省电保活**：前台服务 + 常驻通知栏，引导添加电池优化白名单

### 更新日志

#### v2.0 (2026-05-02)
- **截图流程优化**：VirtualDisplay 持续运行，避免频繁弹窗
- **多指操作修复**：防止多指触摸导致的异常行为
- **屏幕旋转适配**：监听分辨率变化，自动重建截图环境
- **选区取消机制**：用户可中途取消已触发的选区确认
- **历史记录管理**：支持单条删除和清空全部

---

## 环境要求

| 组件 | 最低版本 | 推荐版本 |
|------|---------|---------|
| Android Studio | Arctic Fox (2020.3.x) | Jellyfish (2024.1.1) |
| JDK | 11 | 17 (Gradle 8+ 兼容) |
| minSdk | 29 (Android 10) | - |
| targetSdk | 34 (Android 14) | - |
| Gradle | 7.4 | 8.2+ |
| Android Gradle Plugin | 7.2.2 | 8.1.x |

---

## 快速上手

### 1. 克隆/拉取项目

```bash
git clone https://github.com/C-2-X/AndroidOcrScreen.git
cd AndroidOcrScreen
```

### 2. 配置中国大陆 Gradle 镜像（可选但推荐）

项目已内置阿里云镜像配置，如需全局生效，复制 `init.gradle` 到你的用户目录：

```
# Windows
复制 init.gradle → C:\Users\你的用户名\.gradle\init.gradle
```

### 3. 打开项目

用 Android Studio 打开项目根目录，等待 Gradle Sync 完成。

### 4. 配置百度 OCR

1. 前往 [百度智能云 OCR](https://cloud.baidu.com/doc/OCR/index.html) 注册账号并创建应用
2. 获取 `API Key` 和 `Secret Key`
3. 在手机上安装并打开 App，进入主界面，填入凭证并保存

### 5. 运行 App

1. 连接 Android 设备（或开启模拟器）
2. 点击 Android Studio 「Run」按钮，安装并运行
3. 在 App 主界面：
   - 授予「悬浮窗权限」
   - 授予「无障碍服务」权限（在系统设置开启）
   - 点击「开启悬浮球」
4. 在任意界面点击屏幕边缘的半透明悬浮条，开始使用！

---

## 项目架构

```
AndroidOcrScreen/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/androidpumushibie/
│   │   │   │   ├── MainActivity.java               # 主界面（凭证、设置、历史）
│   │   │   │   ├── OverlayControlService.java      # 核心前台服务（悬浮球、截图、流程控制）
│   │   │   │   ├── OcrAccessibilityService.java   # 无障碍服务保活桩
│   │   │   │   ├── MediaProjectionPermissionActivity.java  # 透明代理，请求屏幕录制权限
│   │   │   │   ├── SelectionOverlayView.java       # 截屏框选自定义 View
│   │   │   │   ├── ResultPanelView.java            # 识别结果浮动面板
│   │   │   │   ├── OverlayUiFactory.java           # 圆角背景等 UI 工厂
│   │   │   │   ├── BaiduOcrApi.java                # Retrofit API 接口
│   │   │   │   ├── BaiduOcrRepository.java         # 百度 OCR 业务逻辑（Token、编码、调用）
│   │   │   │   ├── BaiduOcrResponse.java           # OCR 响应数据模型
│   │   │   │   ├── BaiduTokenResponse.java         # Token 响应数据模型
│   │   │   │   ├── SettingsManager.java            # SharedPreferences 设置管理
│   │   │   │   ├── HistoryRepository.java          # 历史记录存取（支持删除、清空）
│   │   │   │   ├── HistoryAdapter.java             # 历史记录 RecyclerView Adapter
│   │   │   │   └── PermissionHelper.java           # 权限检查与引导工具
│   │   │   ├── res/
│   │   │   │   ├── layout/                         # activity_main.xml, item_history.xml
│   │   │   │   ├── values/strings.xml, colors.xml, themes.xml
│   │   │   │   └── xml/ocr_accessibility_service.xml, network_security_config.xml
│   │   ├── test/                                   # 单元测试
│   │   └── androidTest/                            # 仪器化测试
│   └── build.gradle
├── init.gradle                                       # Gradle 全局镜像配置模板
└── README.md                                        # 本文档
```

---

## 核心组件

### 1. OverlayControlService（核心）

> 路径: `app/src/main/java/com/example/androidpumushibie/OverlayControlService.java`

**职责**:
- 前台服务，通知栏常驻
- 管理屏幕边缘悬浮球的显示/隐藏/拖拽
- 监听屏幕状态（锁屏隐藏，亮屏恢复）
- **持续运行 VirtualDisplay**，避免每次截图都弹窗
- 监听屏幕分辨率变化，自动重建截图环境
- 协调 SelectionOverlayView → BaiduOcrRepository → ResultPanelView

**关键特性**:
- `projectionInitialized`: 标记 VirtualDisplay 是否已初始化
- `DisplayManager.DisplayListener`: 监听屏幕变化
- `captureScreen()`: 快速从缓存的 ImageReader 获取截图

---

### 2. SelectionOverlayView

> 路径: `app/src/main/java/com/example/androidpumushibie/SelectionOverlayView.java`

**职责**:
- 全屏覆盖半透明蒙层
- 展示截屏 Bitmap
- 支持四种交互模式：
  - `MODE_DRAW`: 初始拖拽绘制选区
  - `MODE_MOVE`: 拖拽选区内部整体移动
  - `MODE_LEFT_TOP`, `MODE_RIGHT_TOP`, `MODE_LEFT_BOTTOM`, `MODE_RIGHT_BOTTOM`: 拖拽四角调整大小
- 松手 360ms 后自动确认裁剪
- `loading` 状态显示遮罩

**关键特性**:
- `activePointerId`: 追踪活跃触摸点，防止多指操作混乱
- `confirmed` 标志：防止重复触发 OCR 请求
- ACTION_DOWN 时重置确认状态，支持中途取消

---

### 3. ResultPanelView

> 路径: `app/src/main/java/com/example/androidpumushibie/ResultPanelView.java`

**职责**:
- 可拖拽浮动卡片，高度限制 ≤60% 屏高
- 逐行展示识别文本，每行前带勾选框
- 行末「复制」按钮，底部「全选」「复制全部/复制所选(N)」
- 内容多时支持静默垂直滚动（`ScrollView` + `scrollBarEnabled=false` + `overScroll=never`）
- 保留换行符

---

### 4. BaiduOcrRepository

> 路径: `app/src/main/java/com/example/androidpumushibie/BaiduOcrRepository.java`

**职责**:
- Token 自动获取/缓存/刷新（30天有效期，提前 5 分钟刷新）
- Bitmap → JPEG → Base64 → URL 编码 → MultipartBody 上传
- 10 秒超时控制
- 错误映射（网络未连接/凭证无效/API 错误）
- 自动保存识别成功文本到历史记录（最近 100条）

---

### 5. HistoryRepository

> 路径: `app/src/main/java/com/example/androidpumushibie/HistoryRepository.java`

**职责**:
- 历史记录本地存储（SharedPreferences + Gson）
- 支持单条删除和清空全部

---

## 开发工作流

### 1. 功能开发

1. 创建新分支：`git checkout -b feature/你的功能名`
2. 修改代码，保持风格一致（见下「编码规范」）
3. 测试：
   - 单元测试：`.\gradlew.bat test`
   - 仪器化测试：`.\gradlew.bat connectedAndroidTest`
4. 提交代码：`git add . && git commit -m "feat: 简短描述"`
5. 推送分支并创建 Pull Request

### 2. 调试悬浮球/OverlayView

- 悬浮球不会在常规 Debug 布局预览中显示
- 通过 `Log.d("TAG", ...)` + Android Studio Logcat 调试
- 可以临时在 `MainActivity` 中加触发按钮测试某一步

### 3. 构建 APK

```bash
# Debug
.\gradlew.bat assembleDebug
# 输出: app/build/outputs/apk/debug/app-debug.apk

# Release
.\gradlew.bat assembleRelease
# 输出: app/build/outputs/apk/release/app-release.apk
```

---

## 编码规范

### 1. 语言与风格

- 当前项目主要用 Java
- 遵循标准 Android 命名约定：
  - 类名：大驼峰（`OverlayControlService`）
  - 方法名/变量名：小驼峰（`onHandleClick()`）
  - 常量：全大写下划线（`CHANNEL_ID`）
  - Layout ID：小写下划线（`activity_main`）

### 2. 文件组织

- 按功能分包（当前已做到）
- Layout 和 string 资源与 Java 逻辑分开
- 避免在单个文件超过 600 行

### 3. 注释

- 类级注释：说明整体职责
- 公共 API 注释：说明用途、参数、返回值、异常
- 复杂逻辑块注释：解释意图，避免只解释代码本身

### 4. 错误处理

- 所有异常 Toast 提示，不打断用户
- 尽量保持服务存活，在可恢复时回退
- 网络请求不要阻塞主线程，用 Retrofit/ExecutorService

---

## 测试

### 单元测试

- 位置：`app/src/test/`
- 运行：`.\gradlew.bat test`
- 当前主要是框架模板，待完善业务逻辑测试

### 仪器化测试

- 位置：`app/src/androidTest/`
- 运行：`.\gradlew.bat connectedAndroidTest`
- 测试前提：连接真机/模拟器并授予悬浮窗权限

### 手动测试检查清单

| 测试项 | 通过标准 |
|--------|---------|
| 悬浮球贴靠左右边缘 | 是 |
| 悬浮球可拖拽移动 | 是 |
| 锁屏后隐藏，亮屏后恢复 | 是 |
| 点击悬浮球触发截屏授权（首次） | 是 |
| 后续点击不弹窗（持续 VirtualDisplay） | 是 |
| 框选可拖拽四角调整大小 | 是 |
| 框选可拖拽内部整体移动 | 是 |
| 多指触摸时不会异常 | 是 |
| 屏幕旋转后截图比例正确 | 是 |
| OCR 识别成功并展示 | 是 |
| 逐行勾选、跨行多选可用 | 是 |
| 点击行末复制可复制单行 | 是 |
| 点击复制全部可复制所有（或所选） | 是 |
| 复制后自动关闭面板、恢复悬浮球 | 是 |
| 结果面板可拖拽 | 是 |
| 历史记录保存并展示最近 100 条 | 是 |
| 历史记录支持单条删除 | 是 |
| 历史记录支持清空全部 | 是 |
| 结果面板无垂直滚动条 | 是 |
| 百度 Token 可缓存和自动刷新 | 是 |

---

## 常见问题

### Q1: 为什么会有 MediaProjection 屏幕录制权限弹窗？

A1: 这是 Android 系统安全限制，任何截取屏幕内容的 App 都需要用户显式授权。**首次授权后**，应用会持续运行 VirtualDisplay，后续使用不会再弹窗。

### Q2: 如何修改悬浮球大小/透明度/默认位置？

A2: 在 `MainActivity` 界面有滑块可调，配置通过 `SettingsManager` 持久化。代码默认值在 `SettingsManager.DEFAULT_*`。

### Q3: 百度 Token 经常失效怎么办？

A3: 检查：
1. API Key/Secret Key 是否正确
2. `BaiduOcrRepository` 中 `TOKEN_REFRESH_BUFFER_MS` 是提前刷新时间（当前 5 分钟），可以调大
3. 网络连接是否稳定

### Q4: OCR 上传慢/失败怎么办？

A4: 已从 PNG 改为 JPEG 85% 压缩，减少上传体积。如仍慢，检查：
1. 网络质量
2. 截图区域是否过大

### Q5: Gradle Sync 慢/依赖下载失败？

A5: 项目已内置阿里云镜像配置，全局生效请复制 `init.gradle` 到 `C:\Users\你的用户名\.gradle\init.gradle`。

### Q6: 悬浮球在某些全屏应用（游戏/视频）里看不见？

A6: 检查：
1. 是否已引导用户添加到电池优化白名单
2. 系统是否有「权限限制」（部分国产 ROM）
3. 尝试在 `OverlayControlService` 里微调 `WindowManager.LayoutParams.FLAG_*`

### Q7: 屏幕旋转后截图比例错乱？

A7: 已修复。应用会监听屏幕分辨率变化，自动重建 VirtualDisplay 环境。

### Q8: 多指操作时应用异常？

A8: 已修复。应用会忽略多指触摸，只处理第一个触摸点的事件。

---

## 联系方式

### 维护者

- GitHub: [@C-2-X](https://github.com/C-2-X)
- Issues: https://github.com/C-2-X/AndroidOcrScreen/issues

---

## 许可证

本项目仅供学习和个人使用。
