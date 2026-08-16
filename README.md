# StarDockLauncher

> 我的世界 Java 版 · 新一代安卓启动器 · 全链自主重做 · 保留 Boat/PojavLauncher native 内核

StarDockLauncher v1.1.0 是一款面向 Android 的《Minecraft: Java Edition》启动器（APK，兼容 Android 8.0 – Android 15）。保留开源项目 [PojavLauncher](https://github.com/PojavLauncherTeam/PojavLauncher) / Boat 的 native 内核（OpenGL ES 渲染、JVM 运行时、Modrinth/CurseForge 下载等 54M+1.1G 不可替代资产），从 0 自研启动器 UI 与全部业务层。

> 当前版本：**v1.1.0**（versionCode 12） | 分支：`1.1.0-dev`

## 重大变化（v1.0.9 → v1.1.0）

### 全链自主重做
- **保留 Boat/PojavLauncher native 内核**（native C++，负责 Minecraft JVM 启动、OpenGL 渲染、Modrinth API 客户端）
- **删除 HMCLPE 全部 492 个 Java 文件 + 388MB assets + 全部 res**（anim/color/drawable/layout/menu/mipmap-*/raw/values/values-*/xml + libs/）
- **applicationId 改 `com.stardock.launcher`**（旧为 `com.tungsten.hmclpe`）
- **从 0 重写启动器 UI 与全部业务层**（34 个 Java 文件 / 15 个 layout / Material3 主题）
- **调现有 BoatMinecraftActivity / PojavMinecraftActivity** 启动 MC（通过 Intent 透明代理，旧 module 包名保留）

### APK 体积
- v1.0.9：179.6MB（含 388MB HMCLPE assets + 1.1G JVM 运行时）
- v1.1.0：**26.3MB**（仅保留 Boat/PojavLauncher native + 自研启动器，HMCLPE assets 全删）

## 启动器层（全链自研）

### Activity 入口
- `SplashActivity` —— 5 阶段进度条（运行时 / 资源 / 启动框架 / 检查更新 / 进入启动器），子线程全 try/catch 兜底
- `HomeActivity` —— 4 tab 切换（home/download/tools/setting）+ FAB 开始游戏 + 账户刷新
- `HomeFragment` —— 账号卡片 + 3 个按钮 + 已安装版本 RecyclerView
- `DownloadFragment` —— 下载中心卡片列表
- `ToolsFragment` —— AI 助手快捷入口
- `SettingFragment` —— 液态玻璃多分组设置（外观 / AI / 启动框架 / 协议 / 关于）
- `AccountActivity` —— 三方式登录（离线 / Mojang / Microsoft）
- `VersionsActivity` —— 版本管理
- `UpdateDownloadActivity` —— 检查 GitHub Release + 下载 APK
- `AiChatActivity` —— DeepSeek / OpenAI / 离线演示三模型切换
- `AboutActivity` / `CrashLogViewerActivity` —— 关于 / 崩溃日志查看

### 业务层
- `SDApplication` —— Application + CrashHandler 注册 + 路径初始化
- `CrashHandler` —— 崩溃日志写入 `cache_dir/crash_logs/crash_YYYYMMDD_HHMMSS.log`
- `AppManifest` / `LauncherDirs` —— 运行时 / 缓存 / 游戏 / 版本 / 账户 / AI 配置目录
- `Prefs` —— SharedPreferences 封装
- `AccountInfo` / `AccountManager` / `AuthResult` / `AuthService` —— 离线/Mojang/Microsoft 登录
- `VersionInfo` / `VersionManager` —— 版本管理 + 扫描已安装
- `DownloadSource` / `DownloadTask` / `DownloadService` —— OkHttp 下载（BMCLAPI / Mojang / MCBBS）
- `LauncherUpdate` / `UpdateService` —— GitHub Releases API 检查更新
- `AiProvider` / `AiProviderManager` / `AiChatService` / `AiMessage` —— AI 服务抽象

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

输出：`HMCLPE/build/outputs/apk/release/StarDockLauncher-v1.1.0-release.apk`

## 下载

- [v1.1.0 (全链自主重做)](https://github.com/1953187487/StarDockLauncher/releases/download/v1.1.0/StarDockLauncher-v1.1.0-release.apk) —— 26.3MB
- [v1.0.9 (修复启动闪退)](https://github.com/1953187487/StarDockLauncher/releases/download/v1.0.9/StarDockLauncher-v1.0.9-release.apk) —— 179.6MB
- [历史版本](https://github.com/1953187487/StarDockLauncher/releases)

## v1.1.x 后续规划
- v1.1.1：启动框架选择 UI（Boat / Pojav 切换）+ Modrinth/CurseForge 搜索
- v1.1.2：ModsManagerActivity 重写 + AI 汉化集成
- v1.1.3：游戏内浮窗 + 键位编辑器
