# AndroidPuMuShiBie - 全局 OCR 悬浮球工具

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![minSdk](https://img.shields.io/badge/minSdk-29-red)
![targetSdk](https://img.shields.io/badge/targetSdk-34-blue)

极简 Android 悬浮球工具：点击即触发截屏框选，通过百度 OCR API 识别屏幕标准印刷文本（中英混排），结果支持逐行/跨行多选复制，悬浮球常驻后台（含视频全屏场景）。

---

## 功能特点

- **屏幕边缘悬浮球**：仅贴靠左右边缘，可拖拽移动位置，锁屏自动隐藏
- **一键截屏+框选**：点击悬浮球 → 屏幕冻结 → 半透明蒙层 → 拖拽四角调整大小/选区整体移动
- **百度 OCR 集成**：支持自定义 API Key/Secret Key，自动 Token 缓存，提前刷新避免过期
- **结果面板**：可拖拽浮动卡片，高度自适应，内容多时可静默滚动（无可见滚动条）
- **历史记录**：本地保存最近 100 条识别文本，支持单条删除和清空
- **权限按需申请**：悬浮窗/无障碍/屏幕录制（MediaProjection）按需引导
- **省电保活**：前台服务 + 常驻通知栏，引导添加电池优化白名单

---

## 环境要求

| 组件 | 最低版本 | 推荐版本 |
|------|---------|---------|
| Android Studio | Arctic Fox (2020.3.x) | Jellyfish (2024.1.1) |
| JDK | 11 | 17 |
| minSdk | 29 (Android 10) | - |
| targetSdk | 34 (Android 14) | - |
| Gradle | 7.4 | 8.2+ |
| Android Gradle Plugin | 7.2.2 | 8.1.x |

---

## 快速上手

### 1. 克隆项目

```bash
git clone https://github.com/C-2-X/AndroidPuMuShiBie.git
cd AndroidPuMuShiBie
```

### 2. 配置 Gradle 镜像（可选但推荐）

项目已内置阿里云镜像配置。如需全局生效，复制 `init.gradle` 到用户目录：

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
AndroidPuMuShiBie/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/androidpumushibie/
│   │   │   ├── MainActivity.java               # 主界面（凭证、设置、历史）
│   │   │   ├── OverlayControlService.java      # 核心前台服务
│   │   │   ├── OcrAccessibilityService.java   # 无障碍服务
│   │   │   ├── MediaProjectionPermissionActivity.java  # 屏幕录制权限
│   │   │   ├── SelectionOverlayView.java       # 截屏框选
│   │   │   ├── ResultPanelView.java            # 识别结果面板
│   │   │   ├── BaiduOcrApi.java                # Retrofit API
│   │   │   ├── BaiduOcrRepository.java         # OCR 业务逻辑
│   │   │   ├── HistoryRepository.java           # 历史记录（支持删除）
│   │   │   └── PermissionHelper.java           # 权限工具
│   │   ├── res/                                # 布局、资源
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── init.gradle                                # Gradle 镜像配置
└── README.md
```

---

## 核心组件

### OverlayControlService

核心前台服务，负责：
- 悬浮球显示/隐藏/拖拽
- 持续运行 VirtualDisplay，避免频繁弹窗
- 监听屏幕状态和分辨率变化
- 协调截图→OCR→结果展示流程

### SelectionOverlayView

截屏框选组件：
- 全屏蒙层 + 截图展示
- 四角拖拽调整/整体移动选区
- 多指操作防护
- 松手 360ms 后自动确认

### BaiduOcrRepository

百度 OCR 封装：
- Token 自动获取/缓存/刷新（30天）
- Bitmap → JPEG → Base64 → API 调用
- 10 秒超时控制
- 自动保存历史记录

---

## 构建

```bash
# Debug
.\gradlew.bat assembleDebug

# Release
.\gradlew.bat assembleRelease
```

输出：`app/build/outputs/apk/debug/app-debug.apk`

---

## 常见问题

**Q: 为什么会有屏幕录制权限弹窗？**

A: Android 系统安全限制。首次授权后，VirtualDisplay 持续运行，后续使用不会再弹窗。

**Q: 屏幕旋转后截图比例错乱？**

A: 已修复。应用会监听分辨率变化，自动重建截图环境。

**Q: 多指操作时应用异常？**

A: 已修复。应用只处理第一个触摸点的事件。

---

## 许可证

本项目仅供学习和个人使用。
