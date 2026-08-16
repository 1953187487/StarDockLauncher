# StarDockLauncher

> 我的世界 Java 版 · 新一代安卓启动器 · 横屏导航 / 淘瓦联机 / AI 搜索 / 全能下载

StarDockLauncher 是一款面向 Android 的《Minecraft: Java Edition》启动器（APK，兼容 Android 8.0 – Android 15），以开源项目 [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher) 为内核基座，由 StarDock 团队从 UI 到底层全链重写。核心价值：**横屏侧栏导航、淘瓦（Taowa）内网穿透联机、AI 驱动的资源搜索、下载与模组一体化管理**，让手机玩《我的世界》顺手又优雅。

> 当前版本：**v1.0.6**（versionCode 7） | 分支：`1.0.3-dev`

## 特性

### 导航与布局
- **横屏侧栏导航**：底部导航移到左侧，NavigationRailView 88dp 横排固定侧栏，顶部仅保留联机与 AI 两个入口。
- **Material3 深色主题**：完整色板（surface `#0E1116` / primary `#FFB454`），主题支持系统 / 深色 / 浅色 / Android 12 动态取色 + 自定义背景。
- **图标与动画**：应用图标 100% 重写，启动加载动画升级。

### 淘瓦联机
- **一键开关**：主页联机开关控制，复用 Hin2nService 内网穿透 + TunnelMultiplayerFragment + MultiplayerActivity，自动识别房间并生成邀请码，跨地域好友一起玩。
- **悬浮球菜单**：游戏内悬浮球 → 菜单 → 连接 → 我要当房主，无需输入名称自动识别 Java 版 MC 局域网端口。

### AI 搜索框
- **界面全链重写**：AI 界面改为搜索框，支持直接粘贴 Modrinth / Curseforge 链接识别，或关键词搜索。
- **全类型资源**：搜索 Mod / 光影 / 资源包 / 整合包，点击结果直达下载页。
- **模型管理**：自定义 AI 服务商（OpenAI 兼容接口），支持拉取模型列表 + 在线测试模型，AI 协议独立确认。

### 下载与模组一体化
- **三 Tab 合一**：游戏版本 / 模组 / 整合包统一在下载中心，Modrinth API 搜索。
- **智能版本识别**：模组自动识别本地已安装的游戏版本并优先跳转，一键下载依赖。
- **多下载源**：BMCLAPI / Mojang 官方源可切换，下载目录统一为 `StarDockLauncher/{games,libraries,runtime}`。

### 设置全面完善
- **登录**：正版（Microsoft 微软）账号 / 离线账号 / 第三方验证服务器（authlib-injector）。
- **关于**：一键检查更新 + 历史版本直连开源仓库。
- **协议与公告**：语言选择 → 用户须知协议 → AI 协议，通过后自动弹出公告。

## 版本历史

| 版本 | 版本Code | 说明 |
|------|---------|------|
| **v1.0.6** | 7 | 横屏侧栏 + 淘瓦联机 + AI 搜索框 + 下载/模组合并 + 设置全面完善 |
| v1.0.5 | 6 | UI/架构全面重写，Material3 + 5 Fragment 导航 |
| v1.0.4 | 5 | 修复 8 项 NPE 崩溃（HMCLPEApplication / extras / CrashHandler 等） |
| v1.0.3 | 4 | 首版正式发布 |

## 下载

前往 [GitHub Releases](https://github.com/1953187487/StarDockLauncher/releases) 下载最新 APK（`StarDockLauncher-v1.0.6-release.apk`，约 179.5MB）。

启动器内的"检查更新"会直连本仓库获取 `launcher_version.json` 并自动提示下载。

## 构建

环境要求：JDK 17+、Android SDK 34、Android NDK 21.4.7075529。

```bash
# 本地签名配置在 local.properties 或 gradle 中配置 keystore
./gradlew :HMCLPE:assembleRelease
```

构建产物位于 `HMCLPE/build/outputs/apk/release/`，命名 `StarDockLauncher-v${versionName}-${variant}.apk`。

## 项目结构

```
HMCLPE/                 Android 应用主模块（UI 与业务逻辑，v1.0.6 全链重写）
Boat/                   渲染器 / 运行时组件
jre_lwjgl3glfw/         LWJGL3 + GLFW 运行时组件
forge_installer/        Forge / Fabric 等模组加载器安装器
arc_dns_injector/       DNS 注入组件（网络辅助）
```

## 致谢

本项目的游戏启动内核构建于 PojavLauncher 团队的开源成果之上，感谢其作者与社区贡献者。本项目遵循原开源协议，以二次开发形式发布，仅为非官方学习与使用项目，与 Mojang / Microsoft 无任何关联。《Minecraft》是 Mojang AB 的商标。

## 免责声明

本项目不包含《Minecraft》游戏本体，游戏资源请在登录并启动时由官方渠道下载获取。请遵守当地法律法规与《Minecraft》最终用户许可协议（EULA）。
