# v1.1.0 StarDockLauncher 自研启动器 proguard 规则
# 保留 Boat/PojavLauncher native 入口
-keep class com.tungsten.hmclpe.launcher.launch.boat.BoatMinecraftActivity { *; }
-keep class com.tungsten.hmclpe.launcher.launch.pojav.PojavMinecraftActivity { *; }
