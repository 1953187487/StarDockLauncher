package com.tungsten.hmclpe.ai;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class AiModTranslator {

    public interface Callback {
        void onResult(File zhFile, String text);

        void onError(Throwable t);
    }

    public static void translateMod(Context ctx, File modFile, String versionId, Callback cb) {
        try {
            AiProvider provider = AiProviderManager.get(ctx).active();
            String prompt = "你是 Minecraft 模组汉化专家。请将以下模组文件名翻译成简体中文。\n"
                    + "只返回一个简短的中文名称，不要任何解释、引号或多余文字。\n"
                    + "模组文件：" + modFile.getName()
                    + "\nMinecraft 版本：" + versionId;
            List<AiMessage> history = new ArrayList<>();
            history.add(new AiMessage("user", prompt));
            AiChatService svc = new AiChatService();
            svc.chat(provider, history, new AiChatService.StreamCallback() {
                @Override
                public void onDelta(String delta) {
                }

                @Override
                public void onDone(String full) {
                    try {
                        if (full == null || full.trim().isEmpty()) {
                            full = modFile.getName();
                        }
                        String name = full.trim();
                        File parent = modFile.getParentFile();
                        if (parent != null) {
                            parent.mkdirs();
                        }
                        String base = modFile.getName();
                        int dot = base.lastIndexOf('.');
                        String stem = dot > 0 ? base.substring(0, dot) : base;
                        File out = new File(parent, stem + ".zh.txt");
                        String content = "mod=" + name + "\nsource=" + modFile.getName() + "\n";
                        try (OutputStream os = new FileOutputStream(out)) {
                            os.write(content.getBytes(StandardCharsets.UTF_8));
                        }
                        if (cb != null) {
                            cb.onResult(out, name);
                        }
                    } catch (Throwable t) {
                        if (cb != null) {
                            cb.onError(t);
                        }
                    }
                }

                @Override
                public void onError(Throwable t) {
                    if (cb != null) {
                        cb.onError(t);
                    }
                }
            });
        } catch (Throwable t) {
            if (cb != null) {
                cb.onError(t);
            }
        }
    }
}
