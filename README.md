# StarDockLauncher

> 我的世界 Java 版 · 新一代手机启动器

StarDockLauncher 是一款面向 Android 的《Minecraft：Java Edition》启动器（APK，兼容 Android 8.0 – Android 15），以著名的开源项目 [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)（MIT 协议）源码为基座进行的二次开发与全面 UI 重构。我们在保留其强大启动内核的同时，重写了界面布局、交互结构与扩展模块，让手机玩《我的世界》更加顺手、更加优雅。

## 特性

- **横屏沉浸式界面**：全应用横屏设计，为游戏场景而生。
- **首次使用协议**：安装后首启自动弹出《用户须知与使用协议》，明确确认后进入。
- **一站式启动**：游戏版本管理、Java 运行时自动获取（含 Java 25 等新运行时）、游戏自动下载安装，一键进入游戏。
- **多模式登录**：离线账户、正版（Microsoft）账户、第三方验证服务器（authlib-injector）三种登录方式。
- **下载中心**：基于 Modrinth 检索并一键下载模组、资源包与光影，支持按游戏版本过滤。
- **AI 助手**：支持自定义 AI 服务商（OpenAI 兼容接口），文字 / 长按语音输入提问，语音播报回复，还能让 AI 为你生成键位布局。
- **音乐播放**：内置音乐播放器，支持导入本地音乐文件，边玩边听。
- **联机功能**：局域网联机指引、内网穿透远程联机配置（Tailscale / ZeroTier 等），跨地域好友一起玩。
- **键位系统**：图形化自定义控制布局，AI 生成键位布局并一键保存、导入。
- **渲染器可切换**：内置多套渲染器（GL4ES / Zink / LTW 等），性能与画质自由权衡。

## 屏幕截图

> v0.0.1 预发布版本，界面截图持续更新中。

## 项目结构

```
app_pojavlauncher/    Android 应用主模块（UI 与业务逻辑）
jre_lwjgl3glfw/       LWJGL3 + GLFW 运行时组件
forge_installer/      Forge / Fabric 等模组加载器安装器
arc_dns_injector/     DNS 注入组件（网络辅助）
```

## 构建

环境要求：JDK 17+、Android SDK 34、Android NDK 25.2。

```bash
./gradlew :app_pojavlauncher:assembleDebug
```

构建产物位于 `app_pojavlauncher/build/outputs/apk/debug/`。

## 致谢

本项目的游戏启动内核构建于 PojavLauncher 团队的开源成果之上，感谢其作者与社区贡献者。本项目遵循原开源协议，以二次开发形式发布，仅为非官方学习与使用项目，与 Mojang / Microsoft 无任何关联。《Minecraft》是 Mojang AB 的商标。

## 免责声明

本项目不包含《Minecraft》游戏本体，游戏资源请在登录并启动时由官方渠道下载获取。请遵守当地法律法规与《Minecraft》最终用户许可协议（EULA）。
