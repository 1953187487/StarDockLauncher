# StarDockLauncher

> 我的世界 Java 版 · 新一代安卓启动器 · 全链自主重做 · 保留 Boat/PojavLauncher native 内核

StarDockLauncher v1.1.1 是一款面向 Android 的《Minecraft: Java Edition》启动器（APK，兼容 Android 8.0 – Android 15）。保留开源项目 [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher) / Boat 的 native 内核（OpenGL ES 渲染、JVM 运行时、Modrinth/CurseForge 下载等 54M+1.1G 不可替代资产），从 0 自研启动器 UI 与全部业务层。

> 当前版本：**v1.1.1**（versionCode 13） | 分支：`1.1.0-dev`

## 重大变化（v1.1.0 → v1.1.1）

### 修复进入游戏闪退（v1.1.0 回归问题）
- **恢复 `app_runtime` 运行时资产**：v1.1.0 删除的 388MB 运行时（JRE8 / JRE17 / boat / pojav / caciocavallo，674 个文件）已从 v1.0.9 恢复，内置进 APK。首次启动 `RuntimeInstaller` 自动解压到应用私有目录 `getDir("runtime")/current`，`version` 文件比对避免重复解压。
- **重建 MC 启动封装**：v1.1.0 引用的 `com.tungsten.hmclpe.launcher.launch.boat.BoatMinecraftActivity` / `pojav.PojavMinecraftActivity` 类已随 HMCLPE 删除导致闪退；v1.1.1 新建同名类，直接继承 native 入口：
  - `BoatMinecraftActivity extends cosine.boat.BoatActivity`（`startGame(javaPath, home, highVersion, args, renderer, gameDir)`）
  - `PojavMinecraftActivity extends net.kdt.pojavlaunch.BaseMainActivity`（`init(gameDir, high)` + `startGame(..., glesVersion)`）
- **启动链路修复**：`HomeActivity.startLastGame` / `RuntimeActivity.startGame` 通过真实 Intent 调启动封装（extras 传 versionId / gameDir / server / renderer / ram），不再引用已删除类。

### v1.1.1 新功能
- **启动框架选择（RuntimeActivity）**：Boat / PojavLauncher 引擎单选切换（`RuntimePrefs` 持久化）+ Java 8/17 路径展示 + 已安装版本列表，点「开始游戏」直接进入。
- **Modrinth 搜索（ModrinthActivity）**：Modrinth v2 API，支持关键词搜索 + 类型筛选（模组 / 整合包 / 资源包 / 光影）+ 加载更多 + 一键下载。
- **Mods 管理（ModsManagerActivity）**：扫描本地 mods 目录 + AI 汉化（`AiModTranslator` 调 DeepSeek / OpenAI / 离线演示，生成 `.zh.txt` 备注）。

## 重大变化（v1.0.9 → v1.1.0）

### 全链自主重做
- **保留 Boat/PojavLauncher native 内核**（native C++，负责 Minecraft JVM 启动、OpenGL 渲染、Modrinth API 客户端）
- **删除 HMCLPE 全部 492 个 Java 文件 + 全部 res**（anim/color/drawable/layout/menu/mipmap-*/raw/values/values-*/xml + libs/）
- **applicationId 改 `com.stardock.launcher`**（旧为 `com.tungsten.hmclpe`）
- **从 0 重写启动器 UI 与全部业务层**（34 个 Java 文件 / 15 个 layout / Material3 主题）

### APK 体积
- v1.0.9：179.6MB（含 388MB HMCLPE assets + 1.1G JVM 运行时）
- v1.1.0：26.3MB（HMCLPE assets 全删，含 flash 回归隐患）
- v1.1.1：**174.5MB**（恢复 app_runtime 运行时，完整可启动）

## 启动器层（全链自研）

### Activity 入口
- `SplashActivity` —— 运行时安装（5 阶段进度）+ 完成后进入启动器
- `HomeActivity` —— 4 tab 切换（home/download/tools/setting）+ FAB 开始游戏 + 账户刷新
- `RuntimeActivity` —— 启动框架选择（Boat / Pojav）+ Java 路径 + 版本列表
- `ModrinthActivity` —— Modrinth 搜索下载
- `ModsManagerActivity` —— Mods 管理 + AI 汉化
- `HomeFragment` —— 账号卡片 + 3 个按钮 + 已安装版本 RecyclerView
- `DownloadFragment` —— 下载中心卡片列表（含 Modrinth / Mods 管理入口）
- `ToolsFragment` —— AI 状态 + Modrinth / Mods 管理入口
- `SettingFragment` —— 液态玻璃多分组设置（外观 / AI / 启动框架 / 协议 / 关于）
- `AccountActivity` —— 三方式登录（离线 / Mojang / Microsoft）
- `VersionsActivity` —— 版本管理
- `UpdateDownloadActivity` —— 检查 GitHub Release + 下载 APK
- `AiChatActivity` —— DeepSeek / OpenAI / 离线演示三模型切换
- `AboutActivity` / `CrashLogViewerActivity` —— 关于 / 崩溃日志查看

### 启动封装（v1.1.1 重建）
- `launcher/launch/MinecraftVersion` —— Gson 解析版本 json（mainClass / arguments / libraries / assetIndex）
- `launcher/launch/LaunchArgsBuilder` —— 构建 JVM 启动参数（-Xmx / classpath / library.path / 模板替换 / server）
- `launcher/launch/boat/BoatMinecraftActivity` —— Boat native 入口封装
- `launcher/launch/pojav/PojavMinecraftActivity` —— Pojav native 入口封装
- `runtime/RuntimeInfo` + `launcher/runtime/RuntimeInstaller` + `RuntimePrefs` —— 运行时发现 / 安装 / 引擎选择

### 业务层
- `SDApplication` —— Application + CrashHandler 注册 + 路径初始化
- `CrashHandler` —— 崩溃日志写入 `cache_dir/crash_logs/crash_YYYYMMDD_HHMMSS.log`
- `AppManifest` / `LauncherDirs` —— 运行时 / 缓存 / 游戏 / 版本 / 账户 / AI 配置目录
- `Prefs` —— SharedPreferences 封装
- `AccountInfo` / `AccountManager` / `AuthResult` / `AuthService` —— 离线/Mojang/Microsoft 登录
- `VersionInfo` / `VersionManager` —— 版本管理 + 扫描已安装
- `DownloadSource` / `DownloadTask` / `DownloadService` —— OkHttp 下载
- `LauncherUpdate` / `UpdateService` —— GitHub Releases API 检查更新
- `AiProvider` / `AiProviderManager` / `AiChatService` / `AiMessage` / `AiModTranslator` —— AI 服务抽象 + Mod 汉化

## 主题

### 液态玻璃
- `sd_liquid_bg` —— 135° 深紫渐变 + 双 radial highlight
- `sd_glass_card` —— 半透明白色卡片（圆角 24dp）
- `sd_liquid_fab` —— 浮动按钮
- `Theme.StarDock.Main` —— Material3 DayNight
- `Theme.StarDock.Splash` —— 全屏启动

### 配色
- `sd_primary #FFB454` / `sd_secondary #7DD3FC` / `sd_tertiary #A78BFA`
- `sd_surface_dim #1B1338` / `sd_surface_container #2A1F58` / `sd_on_surface #F4F1FF`

### 应用图标
- 渐变深紫背景 + 橙色五角星 + "SD" 字样（48/72/96/144/192px）

## 构建

```bash
export ANDROID_HOME=/opt/android-sdk
export ANDROID_NDK_HOME=/opt/android-sdk/ndk/21.4.7075529
./gradlew :HMCLPE:assembleRelease --no-daemon -x test
```

输出：`HMCLPE/build/outputs/apk/release/StarDockLauncher-v1.1.1-release.apk`

## 下载

- [v1.1.1 (修复闪退 + 完整版)](https://github.com/1953187487/StarDockLauncher/releases/download/v1.1.1/StarDockLauncher-v1.1.1-release.apk) —— 174.5MB
- [v1.1.0 (全链自主重做)](https://github.com/1953187487/StarDockLauncher/releases/download/v1.1.0/StarDockLauncher-v1.1.0-release.apk) —— 26.3MB
- [历史版本](https://github.com/1953187487/StarDockLauncher/releases)

## v1.1.x 后续规划
- v1.1.2：游戏内浮窗 + 键位编辑器
- v1.1.3：资源中心完善（CurseForge / MCBBS 下载源）
- v1.2.0：多账户云端同步
