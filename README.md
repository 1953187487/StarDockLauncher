# StarDockLauncher

> 我的世界 Java 版 · 移动端启动器

StarDockLauncher 是一款面向 Android 的《Minecraft：Java Edition》启动器（APK，兼容 Android 8.0 – Android 15）。自 v1.0.0 正式版起，本项目以开源启动器 [HMCL-PE](https://github.com/HMCL-dev/HMCL-PE)（**GNU General Public License v3.0**）源码为基座进行二次开发与品牌化改造，在保留其成熟启动内核与丰富功能的基础上，重写了品牌信息、关于 / 反馈 / 捐赠等页面，并持续迭代界面与扩展能力。

> **版本沿革**：早期预测试版（v0.0.1 – v0.0.9）基于 [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher)（MIT 协议）源码二次开发；自 v1.0.0 正式版起切换至 HMCL-PE 基座，并启用 StarDockLauncher 全新品牌。

## 开源协议与法律声明

StarDockLauncher 是 **HMCL-PE**（GPL-3.0）的衍生作品，因此本项目**整体以 GPL-3.0 协议发布**（详见根目录 [`LICENSE`](./LICENSE)）。

按 GPL-3.0 第 5、6 条的义务，本仓库同时提供：

- 根目录 `LICENSE` —— GPL-3.0 协议全文
- 根目录 [`THIRD-PARTY-LICENSES`](./THIRD-PARTY-LICENSES) —— 上游署名、第三方组件、对应源代码获取方式
- App 内「关于」页面 → 「开源许可证」入口 —— 应用内查看 GPL-3.0 全文、第三方声明、本项目仓库与对应 Release tag
- 每个 GitHub Release 附带的 `StarDockLauncher-v<version>-sources.zip` —— 完整对应源代码

如对本项目代码或协议有任何疑问，可在 App 内通过「反馈」入口与我们联系。

## 基于 HMCL-PE 引入了什么

自 v1.0.0 切换基座以来，StarDockLauncher 直接继承并启用了 HMCL-PE 团队以下能力：

- **完整的游戏启动内核**：基于 PojavLauncher 内核的 JVM 启动、OpenGL 渲染桥接、输入桥接与资源加载链路，整体通过 `PojavLauncher` 模块复用。
- **多版本下载与加载器支持**：官方正式版 / 快照版 / 远古版本一键下载；Fabric、Forge、OptiFine、Quilt、LiteLoader 等加载器全自动安装。
- **下载中心**：Modrinth / CurseForge / MCBBS 多来源的模组检索，资源包、光影、整合包、地图存档统一管理。
- **多下载源**：官方源 / BMCLAPI / MCBBS 镜像自由切换，国内下载更稳定。
- **多模式登录**：离线账户、Microsoft 正版账户、Mojang 账户、authlib-injector 第三方验证服务器。
- **游戏管理器**：模组启用 / 禁用 / 删除 / 更新检查，世界存档创建、导入与导出，版本独立设置。
- **Java 运行时自动获取**：Java 8 / 17 / 21 / 25 运行时自动下载安装，按版本自动匹配。
- **多套渲染器**：GL4ES 与 VirGL 渲染器（Pojav / Boat 两个内核）可按需切换，兼容不同 GPU 与驱动。
- **图形化控制布局**：键位自定义、按键映射与控制方案导入。
- **多语言与外观自定义**：简体中文、繁体中文、英文等多种语言；启动器背景与主题可切换。
- **自动更新检测**：启动器在线版本检测，一键跳转下载最新版本。

## 特性

### 游戏安装与启动

- **一站式安装**：游戏版本一键下载安装，官方正式版 / 快照版 / 远古版本全覆盖。
- **加载器自动安装**：Fabric、Forge、OptiFine、Quilt、LiteLoader 等加载器全自动安装。
- **多版本共存**：版本列表统一管理，每个版本独立配置、自由切换、单独启动。
- **离线可玩**：本地版本与 Java 运行时就绪后即可离线启动，不受网络限制。

### 下载中心

- **模组下载**：聚合 Modrinth / CurseForge / MCBBS 多来源检索，按游戏版本、分类、作者、热度排序过滤。
- **资源包与光影**：资源包、光影一键下载，支持版本匹配与依赖检测。
- **整合包与地图**：整合包（.zip）一键安装，地图存档直接下载导入。
- **多下载源**：官方源 / BMCLAPI / MCBBS 镜像自由切换，下载更稳定。

### 账户系统

- **多模式登录**：离线账户、正版（Microsoft）账户、第三方验证服务器（authlib-injector）、Mojang 账户四种方式。
- **账户管理**：多账户并存、一键切换、自动刷新，支持角色选择。

### 游戏资源管理

- **模组管理器**：已装模组的启用 / 禁用 / 删除 / 更新检查，版本冲突一目了然。
- **世界管理器**：世界存档的创建、导入与导出。
- **版本设置**：每个版本单独配置 Java、内存、分辨率、渲染器等。
- **自动安装**：本地安装包（Forge / Fabric 等）自动识别导入。

### 运行设置

- **Java 运行时自动获取**：Java 8 / 17 / 21 / 25 运行时自动下载安装，按版本自动匹配。
- **内存分配**：图形化内存滑条，按需分配，自动推荐。
- **渲染器切换**：Pojav / Boat 两套内核各支持 GL4ES 与 VirGL 渲染器，性能与画质自行权衡。
- **控制布局**：图形化自定义按键布局，适配手机触屏操作。

### 启动器设置

- **多语言支持**：内置简体中文、繁体中文、英文等多种语言。
- **外观自定义**：启动器背景与主题可切换。
- **下载设置**：下载源、并发数等按需配置。
- **联机功能**：局域网联机指引、内网穿透远程联机配置，跨地域好友一起玩。
- **版本更新检测**：内置在线更新检测，一键跳转下载最新版本。

## 屏幕截图

> 截图持续更新中。

## 项目结构

当前版本（v1.0.0+，基于 HMCL-PE）的主要模块：

```
HMCLPE/           Android 应用主模块（UI、启动、下载、联机、设置等）
PojavLauncher/    游戏启动内核组件（JVM 启动、渲染、输入桥接等）
Boat/             Boat 渲染器组件（替代启动内核的可选实现）
FilePicker/       文件选择器组件
ZipTools/         压缩解压工具组件
```

> 早期预测试版（v0.0.x，基于 PojavLauncher）的模块结构为 `app_pojavlauncher`（主模块）、`jre_lwjgl3glfw`、`forge_installer`、`arc_dns_injector`，相关代码归档于 `PojavLauncher_legacy/`，仅作历史参考。

## 构建

环境要求：JDK 17+、Android SDK 34、Android NDK 25.2。

```bash
./gradlew :HMCLPE:assembleDebug
```

构建产物位于 `HMCLPE/build/outputs/apk/debug/`。

## 版本与更新

版本发布遵循 [语义化版本](https://semver.org/lang/zh-CN/)：大版本切换基座或重大功能，小版本迭代功能，补丁号修复问题。启动器内置在线更新检测，应用内即可收到新版本提示。

## 致谢

本项目的游戏启动内核自 v1.0.0 起构建于 HMCL-PE 团队的开源成果之上，早期预测试版（v0.0.x）构建于 PojavLauncher 团队的开源成果之上，感谢两个项目作者与社区贡献者。本项目遵循对应开源协议，以二次开发形式发布，仅为非官方学习与使用项目，与 Mojang / Microsoft 无任何关联。《Minecraft》是 Mojang AB 的商标。

## 免责声明

本项目不包含《Minecraft》游戏本体，游戏资源请在登录并启动时由官方渠道下载获取。请遵守当地法律法规与《Minecraft》最终用户许可协议（EULA）。

---

*为当初没有更新的遗憾而复活。*
