# StarDockLauncher

> 我的世界 Java 版 · 新一代安卓启动器 · 淘瓦联机 / AI 资源搜索 / 启动框架可选 / 全能下载

StarDockLauncher 是一款面向 Android 的《Minecraft: Java Edition》启动器（APK，兼容 Android 8.0 – Android 15），以开源项目 [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher) 为内核基座，由 StarDock 团队从 UI 到底层全链重写。核心价值：**横屏侧栏导航、淘瓦（Taowa）内网穿透联机、AI 搜索资源、下载与模组一体化、可调节启动框架**，让手机玩《我的世界》顺手又优雅。

> 当前版本：**v1.0.7**（versionCode 8） | 分支：`1.0.3-dev`

## 特性

### 导航与布局（v1.0.7 重规划）
- **侧栏菜单**：主页 / 下载 / 工具 / 设置（删除冗余的版本/模组两个 Tab）
- **顶栏左侧**：圆形账号头像（未登录显示默认占位图，点击进入设置）
- **顶栏右侧**：已精简（删除联机和 AI 两个入口）
- **AI 启动器按钮**：右下角悬浮 FAB（56dp），点击进入 AI 搜索框
- **Material3 深色主题**：完整色板（surface `#0E1116` / primary `#FFB454`），主题支持系统 / 深色 / 浅色 / Android 12 动态取色 + 自定义背景

### 淘瓦联机（v1.0.7 彻底重构）
- **工具页一键开关**：MaterialSwitch 控制，默认关闭
- **关闭时不显示淘瓦入口**：开启才显示
- **复用 Hin2nService 内网穿透 + TunnelMultiplayerFragment**：自动识别房间并生成邀请码
- **删除主页虚假联机开关**：v1.0.5 局域网思路彻底清除

### 工具页（v1.0.7 新增）
- **淘瓦联机开关 + 进入按钮**
- **还原 MC 移动版默认键位**：直接还原网易 MC Mobile 键位
- **打开键位编辑器**：复用 HMCLPE 原有 ControlPatternActivity
- **AI 生成键位描述**：描述想要的布局，AI 给出键位建议

### 下载中心（v1.0.7 大改）
- **4 个独立 Tab**：游戏版本 / 模组 / 整合包 / 光影
- **搜索源选择**：BMCLAPI（仅游戏版本）/ Modrinth / Curseforge
- **默认推荐**：进入 Tab 即显示热门 5+ 项
- **实时搜索建议**：输入 ≥2 字符触发 live 搜索
- **AI 搜索框集成**：Modrinth/Curseforge 全链路
- **修复 MinecraftVersionService**：路径改为 `mc/game/<ver>/client.jar`

### 设置全面完善
- **启动框架**（v1.0.7 新增）：Java 运行时（默认/8/11/17/21/25）+ 渲染器（默认/GL4ES/Zink/LTW/ANGLE）+ 图形驱动（默认/OpenGL ES/Vulkan）
- **登录**：正版（Microsoft 微软）账号 / 离线账号 / 第三方验证服务器（authlib-injector）
- **关于**：一键检查更新（v1.0.7 修复只检测到 v1.0.2 的 bug，直连 GitHub Releases API）+ 历史版本直连开源仓库
- **协议与公告**：语言选择 → 用户须知协议 → AI 协议，通过后自动弹出公告

## 版本历史

| 版本 | 版本Code | 说明 |
|------|---------|------|
| **v1.0.7** | 8 | 界面重新规划 + 淘瓦联机开关 + AI 集成下载 + 启动框架设置 + 检查更新修复 |
| v1.0.6 | 7 | 横屏侧栏 + 淘瓦联机 + AI 搜索框 + 下载/模组合并 + 设置全面完善 |
| v1.0.5 | 6 | UI/架构全面重写，Material3 + 5 Fragment 导航 |
| v1.0.4 | 5 | 修复 8 项 NPE 崩溃（HMCLPEApplication / extras / CrashHandler 等） |
| v1.0.3 | 4 | 首版正式发布 |

## 下载

前往 [GitHub Releases](https://github.com/1953187487/StarDockLauncher/releases) 下载最新 APK（`StarDockLauncher-v1.0.7-release.apk`，约 179.5MB）。

启动器内的"检查更新"会直连本仓库获取 `launcher_version.json` 并自动提示下载（含一键下载 APK 按钮）。

## 构建

环境要求：JDK 17+、Android SDK 34、Android NDK 21.4.7075529。

```bash
# 本地签名配置在 local.properties 或 gradle 中配置 keystore
./gradlew :HMCLPE:assembleRelease
```

构建产物位于 `HMCLPE/build/outputs/apk/release/`，命名 `StarDockLauncher-v${versionName}-${variant}.apk`。

## 项目结构

```
HMCLPE/                 Android 应用主模块（UI 与业务逻辑，v1.0.6+ 全链重写）
Boat/                   渲染器 / 运行时组件
jre_lwjgl3glfw/         LWJGL3 + GLFW 运行时组件
forge_installer/        Forge / Fabric 等模组加载器安装器
arc_dns_injector/       DNS 注入组件（网络辅助）
```

## 致谢

本项目的游戏启动内核构建于 PojavLauncher 团队的开源成果之上，感谢其作者与社区贡献者。本项目遵循原开源协议，以二次开发形式发布，仅为非官方学习与使用项目，与 Mojang / Microsoft 无任何关联。《Minecraft》是 Mojang AB 的商标。

## 免责声明

本项目不包含《Minecraft》游戏本体，游戏资源请在登录并启动时由官方渠道下载获取。请遵守当地法律法规与《Minecraft》最终用户许可协议（EULA）。
