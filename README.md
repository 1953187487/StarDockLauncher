# StarDockLauncher

> 我的世界 Java 版 · 新一代手机启动器

StarDockLauncher 是一款面向 Android 的《Minecraft：Java Edition》启动器（APK，兼容 Android 8.0 – Android 15）。自 v1.0.0 起，项目以开源启动器 [HMCL-PE](https://github.com/HMCL-dev/HMCL-PE)（GPL-3.0 协议）源码为基座进行二次开发与品牌化改造，在保留其成熟启动内核与丰富功能的基础上，重写了品牌信息、关于 / 反馈 / 捐赠等页面，并持续迭代界面与扩展能力。

> 版本沿革：早期预测试版（v0.0.1 – v0.0.9）基于 [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)（MIT 协议）源码二次开发；自 v1.0.0 正式版起切换至 HMCL-PE 基座。

## 特性

- **一站式安装游戏**：游戏版本一键下载安装，支持 Fabric / Forge / OptiFine / Quilt / LiteLoader 等加载器自动安装。
- **完整下载中心**：基于 Modrinth / CurseForge 检索并一键下载模组、资源包、光影、整合包与地图存档，支持按游戏版本、分类与来源过滤。
- **多模式登录**：离线账户、正版（Microsoft）账户、第三方验证服务器（authlib-injector）三种登录方式。
- **游戏资源管理**：内置游戏管理器，统一管理模组、世界存档、资源包，支持本地资源导入与整合包（.zip）安装、世界导出。
- **多版本共存**：游戏版本列表管理，多版本独立配置，自由切换。
- **Java 运行时自动获取**：Java 8 / 17 / 21 / 25 等运行时自动下载安装，按游戏版本自动匹配。
- **全局游戏设置**：内存分配、渲染器切换、分辨率、控制布局等设置项覆盖全部游戏版本。
- **联机功能**：局域网联机指引、内网穿透远程联机配置，跨地域好友一起玩。
- **版本更新检测**：内置启动器在线更新检测，一键跳转下载最新版本。

## 屏幕截图

> v0.0.1 预发布版本，界面截图持续更新中。

## 项目结构

当前版本（v1.0.0+，基于 HMCL-PE）的主要模块：

```
HMCLPE/           Android 应用主模块（UI、启动、下载、联机、设置等）
PojavLauncher/    游戏启动内核组件（JVM 启动、渲染、输入桥接等）
Boat/             渲染器组件
FilePicker/       文件选择器组件
ZipTools/         压缩解压工具组件
```

> 早期预测试版（v0.0.x，基于 PojavLauncher）的模块结构为 `app_pojavlauncher`（主模块）、`jre_lwjgl3glfw`、`forge_installer`、`arc_dns_injector`。

## 构建

环境要求：JDK 17+、Android SDK 34、Android NDK 25.2。

```bash
./gradlew :HMCLPE:assembleDebug
```

构建产物位于 `HMCLPE/build/outputs/apk/debug/`。

## 致谢

本项目的游戏启动内核自 v1.0.0 起构建于 HMCL-PE 团队的开源成果之上，早期预测试版（v0.0.x）构建于 PojavLauncher 团队的开源成果之上，感谢两个项目作者与社区贡献者。本项目遵循对应开源协议，以二次开发形式发布，仅为非官方学习与使用项目，与 Mojang / Microsoft 无任何关联。《Minecraft》是 Mojang AB 的商标。

## 免责声明

本项目不包含《Minecraft》游戏本体，游戏资源请在登录并启动时由官方渠道下载获取。请遵守当地法律法规与《Minecraft》最终用户许可协议（EULA）。
