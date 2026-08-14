package com.tungsten.hmclpe.launcher.uis.game.download;

import android.content.Context;

import com.tungsten.hmclpe.launcher.setting.launcher.child.SourceSetting;
import com.tungsten.hmclpe.utils.io.NetPingManager;

public class DownloadUrlSource {

    public static int DOWNLOAD_URL_SOURCE_OFFICIAL = 0;
    public static int DOWNLOAD_URL_SOURCE_BMCLAPI = 1;
    public static int DOWNLOAD_URL_SOURCE_MCBBS = 2;

    public static int VERSION_MANIFEST = 0;
    public static int VERSION_JSON = 1;
    public static int VERSION_JAR = 2;
    public static int ASSETS_INDEX_JSON = 3;
    public static int ASSETS_OBJ = 4;
    public static int LIBRARIES = 5;
    public static int FORGE_LIBRARIES = 6;

    public static String[] OFFICIAL_URLS = {
            "https://piston-meta.mojang.com/mc/game/version_manifest.json",
            "https://piston-meta.mojang.com",
            "https://piston-data.mojang.com",
            "https://piston-meta.mojang.com",
            "https://resources.download.minecraft.net",
            "https://libraries.minecraft.net",
            "https://maven.minecraftforge.net"
    };

    public static String[] BMCLAPI_URLS = {
            "https://bmclapi2.bangbang93.com/mc/game/version_manifest.json",
            "https://bmclapi2.bangbang93.com",
            "https://bmclapi2.bangbang93.com",
            "https://bmclapi2.bangbang93.com",
            "https://bmclapi2.bangbang93.com/assets",
            "https://bmclapi2.bangbang93.com/maven",
            "https://bmclapi2.bangbang93.com/maven"
    };

    public static String[] MCBBS_URLS = {
            "https://download.mcbbs.net/mc/game/version_manifest.json",
            "https://download.mcbbs.net",
            "https://download.mcbbs.net",
            "https://download.mcbbs.net",
            "https://download.mcbbs.net/assets",
            "https://download.mcbbs.net/maven",
            "https://download.mcbbs.net/maven"
    };

    public static String replaceSubUrl(String url , int source , int type){
        StringBuilder stringBuilder = new StringBuilder(url);
        return stringBuilder.replace(0,getSubUrl(DOWNLOAD_URL_SOURCE_OFFICIAL,type).length(),getSubUrl(source,type)).toString();
    }

    public static String getSubUrl(int source , int type){
        if (source == DOWNLOAD_URL_SOURCE_OFFICIAL){
            return OFFICIAL_URLS[type];
        }
        else if (source == DOWNLOAD_URL_SOURCE_BMCLAPI){
            return BMCLAPI_URLS[type];
        }
        else {
            return MCBBS_URLS[type];
        }
    }

    public static volatile long official = 0;
    public static volatile long bmclapi = 0;
    public static volatile long mcbbs = 0;
    public static volatile boolean pingFinished = false;

    public static void getBalancedSource(Context context){
        // 重新启用自动测速选源。修复原逻辑中 mcbbs 计数被错误累加到 bmclapi、
        // 以及三个源全部 ping 失败时无回退导致的连接/下载源问题。
        official = 0;
        bmclapi = 0;
        mcbbs = 0;
        pingFinished = false;

        NetPingManager officialNetPingService = new NetPingManager(context, "piston-meta.mojang.com", new NetPingManager.IOnNetPingListener() {
            @Override
            public void onDelay(NetPingManager netPingManager,long log) {
                official = log;
                netPingManager.release();
            }

            @Override
            public void onError(NetPingManager netPingManager) {
                official = Long.MAX_VALUE;
                netPingManager.release();
            }
        });
        NetPingManager bmclapiNetPingService = new NetPingManager(context, "bmclapi2.bangbang93.com", new NetPingManager.IOnNetPingListener() {
            @Override
            public void onDelay(NetPingManager netPingManager,long log) {
                bmclapi = log;
                netPingManager.release();
            }

            @Override
            public void onError(NetPingManager netPingManager) {
                bmclapi = Long.MAX_VALUE;
                netPingManager.release();
            }
        });
        NetPingManager mcbbsNetPingService = new NetPingManager(context, "download.mcbbs.net", new NetPingManager.IOnNetPingListener() {
            @Override
            public void onDelay(NetPingManager netPingManager,long log) {
                mcbbs = log;
                netPingManager.release();
            }

            @Override
            public void onError(NetPingManager netPingManager) {
                mcbbs = Long.MAX_VALUE;
                netPingManager.release();
            }
        });
        officialNetPingService.startGetDelay();
        bmclapiNetPingService.startGetDelay();
        mcbbsNetPingService.startGetDelay();

        // 标记测速已发起。getSource 会在结果未就绪时回退到稳定的 BMCLAPI 镜像。
        new Thread(() -> {
            try { Thread.sleep(8000); } catch (InterruptedException ignored) {}
            pingFinished = true;
        }).start();
    }

    public static int getSource(SourceSetting sourceSetting) {
        if (sourceSetting.autoSelect) {
            if (sourceSetting.autoSourceType == 0) {
                // 优先官方源
                return 0;
            }
            else if (sourceSetting.autoSourceType == 1) {
                // 自动测速：选取延迟最低的可用源。
                // 若测速尚未完成或全部失败，回退到国内稳定的 BMCLAPI 镜像。
                if (!pingFinished || (official == 0 && bmclapi == 0 && mcbbs == 0)) {
                    return 1;
                }
                long faster = Math.min(official, bmclapi);
                long fastest = Math.min(faster, mcbbs);
                if (fastest == official) {
                    return 0;
                }
                else if (fastest == bmclapi) {
                    return 1;
                }
                else {
                    return 2;
                }
            }
            else {
                // autoSourceType == 2：国内镜像优先
                return 1;
            }
        }
        else {
            return sourceSetting.fixSourceType;
        }
    }

}
