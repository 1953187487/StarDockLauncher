package com.tungsten.hmclpe.launcher.download.forge;

import android.os.AsyncTask;

import com.tungsten.hmclpe.R;
import com.tungsten.hmclpe.launcher.MainActivity;
import com.tungsten.hmclpe.launcher.list.install.DownloadTaskListAdapter;
import com.tungsten.hmclpe.launcher.list.install.DownloadTaskListBean;
import com.tungsten.hmclpe.launcher.uis.game.download.DownloadUrlSource;
import com.tungsten.hmclpe.manifest.AppManifest;
import com.tungsten.hmclpe.task.DownloadTask;
import com.tungsten.hmclpe.utils.file.FileUtils;
import com.tungsten.hmclpe.utils.io.DownloadUtil;
import com.tungsten.hmclpe.utils.io.SSLSocketClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ForgeDownloadTask extends AsyncTask<ForgeVersion,Integer,Exception> {

    private MainActivity activity;
    private DownloadTaskListAdapter adapter;
    private DownloadForgeCallback callback;

    private DownloadTaskListBean bean;

    public ForgeDownloadTask (MainActivity activity,DownloadTaskListAdapter adapter, DownloadForgeCallback callback) {
        this.activity = activity;
        this.adapter = adapter;
        this.callback = callback;

        this.bean = new DownloadTaskListBean(activity.getString(R.string.dialog_install_game_download_forge),"","","");
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        callback.onStart();
        if (!isCancelled()) adapter.addDownloadTask(bean);
    }

    @Override
    protected Exception doInBackground(ForgeVersion... forgeVersions) {
        ForgeVersion forgeVersion = forgeVersions[0];
        String mirror = null;
        try {
            String baseUrl = "https://bmclapi2.bangbang93.com/forge/download/" + forgeVersion.getBuild();
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            builder.sslSocketFactory(SSLSocketClient.getSSLSocketFactory(),(X509TrustManager) SSLSocketClient.getTrustManager()[0]);
            builder.hostnameVerifier(SSLSocketClient.getHostnameVerifier());
            builder.connectTimeout(10, TimeUnit.SECONDS);
            builder.readTimeout(10, TimeUnit.SECONDS);
            builder.writeTimeout(10, TimeUnit.SECONDS);
            builder.followRedirects(false);
            builder.followSslRedirects(true);
            OkHttpClient okHttpClient = builder.build();
            String redirectUrl = followForgeRedirect(okHttpClient, baseUrl, 0);
            if (redirectUrl == null) {
                throw new IOException("Failed to resolve Forge installer redirect");
            }
            String base;
            int downloadSource = DownloadUrlSource.getSource(activity.launcherSetting.downloadUrlSource);
            if (downloadSource == 0) {
                base = "https://maven.minecraftforge.net";
            }
            else if (downloadSource == 1) {
                base = "https://bmclapi2.bangbang93.com/maven";
            }
            else {
                base = "https://download.mcbbs.net/maven";
            }
            mirror = base + redirectUrl;
            if (mirror == null) {
                throw new IOException("Forge installer URL could not be resolved from " + baseUrl);
            }
        } catch (IOException e) {
            e.printStackTrace();
            if (!isCancelled()) return e;
        }
        if (mirror == null) {
            return new Exception("Failed to resolve Forge installer URL");
        }
        String path = AppManifest.INSTALL_DIR + "/forge/forge-installer.jar";
        FileUtils.deleteDirectory(AppManifest.INSTALL_DIR);
        DownloadTaskListBean bean = new DownloadTaskListBean("forge-installer.jar", mirror,path,null);
        DownloadTask.DownloadFeedback feedback = new DownloadTask.DownloadFeedback() {
            @Override
            public void updateProgress(long curr, long max) {
                long progress = 100 * curr / max;
                bean.progress = (int) progress;
                activity.runOnUiThread(() -> {
                    if (!isCancelled()) adapter.onProgress(bean);
                });
            }

            @Override
            public void updateSpeed(String speed) {

            }
        };
        for (int i = 0;i < 5;i++) {
            try {
                activity.runOnUiThread(() -> {
                    if (!isCancelled()) adapter.addDownloadTask(bean);
                });
                if (DownloadUtil.downloadFile(mirror,path,null,feedback)) {
                    activity.runOnUiThread(() -> {
                        if (!isCancelled()) adapter.onComplete(bean);
                    });
                    if (!isCancelled()) return null;
                }
                else {
                    activity.runOnUiThread(() -> {
                        if (!isCancelled()) adapter.onComplete(bean);
                    });
                    if (i == 4) {
                        if (!isCancelled()) return new Exception("Failed to download forge-installer.jar");
                    }
                }
            }
            catch (IOException e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> {
                    if (!isCancelled()) adapter.onComplete(bean);
                });
                if (i == 4) {
                    if (!isCancelled()) return e;
                }
            }
        }
        if (!isCancelled()) return new Exception("Unknown error");
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Exception e) {
        super.onPostExecute(e);
        adapter.onComplete(bean);
        callback.onFinish(e);
    }

    public interface DownloadForgeCallback{
        void onStart();
        void onFinish(Exception e);
    }

    private static String followForgeRedirect(OkHttpClient client, String url, int depth) throws IOException {
        if (depth > 10) {
            throw new IOException("Too many Forge redirect hops");
        }
        Request request = new Request.Builder().url(url).build();
        Response response = client.newCall(request).execute();
        try {
            int code = response.code();
            if (code == 200) {
                return url.startsWith("https://maven.minecraftforge.net/")
                        ? url.substring("https://maven.minecraftforge.net".length())
                        : url;
            }
            if (code >= 300 && code <= 307 && code != 304 && code != 306) {
                String location = response.header("Location");
                if (location == null || location.isEmpty()) {
                    throw new IOException("Forge installer redirect has no Location header (HTTP " + code + ")");
                }
                if (!location.startsWith("http://") && !location.startsWith("https://")) {
                    String base = url.substring(0, url.indexOf("/", "https://".length() + 1));
                    location = base + location;
                }
                return followForgeRedirect(client, location, depth + 1);
            }
            throw new IOException("Unexpected Forge installer response: HTTP " + code);
        } finally {
            response.close();
        }
    }
}
