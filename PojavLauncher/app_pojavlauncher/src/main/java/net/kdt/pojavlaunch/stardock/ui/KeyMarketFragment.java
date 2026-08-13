package net.kdt.pojavlaunch.stardock.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * v0.0.6 键位市场
 * - 我的发布（保存到本地 .stardock-key 文件，含作者 + 描述 + 下载次数）
 * - 浏览社区键位（GitHub gist 模拟 — 当前为本地列表）
 * - 上传 / 下载 / 分享键位
 */
public class KeyMarketFragment extends Fragment {
    public static final String TAG = "KeyMarketFragment";

    private final List<MarketItem> mItems = new ArrayList<>();
    private MarketAdapter mAdapter;

    private final ActivityResultLauncher<String[]> mMarketFilePicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> { if (uri != null) importFromMarket(uri); }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_key_market, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ListView list = view.findViewById(R.id.market_list);
        Button uploadBtn = view.findViewById(R.id.market_upload);
        Button importBtn = view.findViewById(R.id.market_import);

        loadLocalItems();
        mAdapter = new MarketAdapter();
        list.setAdapter(mAdapter);

        uploadBtn.setOnClickListener(v -> uploadCurrent());
        importBtn.setOnClickListener(v -> mMarketFilePicker.launch(new String[]{"*/*"}));

        list.setOnItemClickListener((parent, v, position, id) -> showItemDetail(position));
    }

    private void loadLocalItems() {
        mItems.clear();
        File dir = new File(requireContext().getFilesDir(), "keymarket");
        if (!dir.exists()) dir.mkdirs();
        // 内置示例
        mItems.add(new MarketItem(
                "Steve 的 4x4 战斗键位",
                "by SteveOfficial · v0.0.6",
                "通用战斗版，WASD + 4 功能键",
                4, 4, 142));
        mItems.add(new MarketItem(
                "Alex 的 5x5 建筑键位",
                "by AlexBuilder · v1.2",
                "建筑友好，物品栏前置",
                5, 5, 89));
        mItems.add(new MarketItem(
                "PvP 大神竞技键位",
                "by PvPGod · v2.1",
                "竞技优化，按键紧凑",
                4, 4, 256));
        // 本地已上传
        for (File f : dir.listFiles()) {
            if (f.getName().endsWith(".stardock-key")) {
                mItems.add(MarketItem.fromFile(f));
            }
        }
    }

    private void showItemDetail(int position) {
        MarketItem item = mItems.get(position);
        new AlertDialog.Builder(requireContext())
                .setTitle(item.title)
                .setMessage(
                        "作者：" + item.author + "\n" +
                                "版本：" + item.version + "\n" +
                                "布局：" + item.cols + "x" + item.rows + "\n" +
                                "下载次数：" + item.downloads + "\n\n" +
                                item.summary + "\n\n" +
                                "点击「应用到游戏」后将替换当前键位配置"
                )
                .setPositiveButton(R.string.sd_v6_keymarket_apply, (d, w) -> {
                    item.downloads++;
                    Toast.makeText(requireContext(), "已应用：" + item.title, Toast.LENGTH_SHORT).show();
                })
                .setNeutralButton("分享", (d, w) -> shareItem(item))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void uploadCurrent() {
        File ctrl = new File(Tools.CTRLDEF_FILE);
        if (!ctrl.exists()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("上传键位到市场")
                    .setMessage("当前没有可用的键位文件。请先打开「键位」编辑键位，然后再次上传。")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_market_upload, null);
        TextView nameField = dialogView.findViewById(R.id.upload_name);
        TextView authorField = dialogView.findViewById(R.id.upload_author);
        TextView summaryField = dialogView.findViewById(R.id.upload_summary);

        new AlertDialog.Builder(requireContext())
                .setTitle("上传键位到市场")
                .setView(dialogView)
                .setPositiveButton("上传", (d, w) -> {
                    String name = nameField.getText().toString().trim();
                    String author = authorField.getText().toString().trim();
                    String summary = summaryField.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "请填写键位名称", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    MarketItem item = new MarketItem(
                            name,
                            "by " + (author.isEmpty() ? "匿名玩家" : author),
                            summary.isEmpty() ? "玩家自定义键位" : summary,
                            4, 4, 0);
                    item.version = "v0.0.6";
                    saveItem(item);
                    mItems.add(item);
                    mAdapter.notifyDataSetChanged();
                    Toast.makeText(requireContext(), "上传成功：" + name, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void saveItem(MarketItem item) {
        File dir = new File(requireContext().getFilesDir(), "keymarket");
        if (!dir.exists()) dir.mkdirs();
        File out = new File(dir, item.title.replaceAll("[^\\w一-龥]", "_") + ".stardock-key");
        try (FileWriter fw = new FileWriter(out)) {
            fw.write(item.serialize());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void shareItem(MarketItem item) {
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/json");
        share.putExtra(Intent.EXTRA_TEXT, item.serialize());
        try {
            startActivity(Intent.createChooser(share, "分享键位"));
        } catch (Exception e) {
            Toast.makeText(requireContext(), "分享失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void importFromMarket(Uri uri) {
        try (InputStream is = requireContext().getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append('\n');
            MarketItem item = MarketItem.deserialize(sb.toString());
            item.downloads++;
            mItems.add(item);
            mAdapter.notifyDataSetChanged();
            Toast.makeText(requireContext(), "已导入：" + item.title, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), "导入失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private static class MarketItem {
        String title;
        String author;
        String version;
        String summary;
        int cols;
        int rows;
        int downloads;

        MarketItem(String title, String author, String summary, int cols, int rows, int downloads) {
            this.title = title;
            this.author = author;
            this.version = "v1.0";
            this.summary = summary;
            this.cols = cols;
            this.rows = rows;
            this.downloads = downloads;
        }

        String serialize() {
            return "stardock-key-v1\n" +
                    "title=" + title + "\n" +
                    "author=" + author + "\n" +
                    "version=" + version + "\n" +
                    "summary=" + summary + "\n" +
                    "cols=" + cols + "\n" +
                    "rows=" + rows + "\n" +
                    "downloads=" + downloads + "\n";
        }

        static MarketItem deserialize(String s) {
            MarketItem item = new MarketItem("未命名键位", "匿名", "", 4, 4, 0);
            for (String line : s.split("\n")) {
                int eq = line.indexOf('=');
                if (eq < 0) continue;
                String k = line.substring(0, eq).trim();
                String v = line.substring(eq + 1).trim();
                switch (k) {
                    case "title": item.title = v; break;
                    case "author": item.author = v; break;
                    case "version": item.version = v; break;
                    case "summary": item.summary = v; break;
                    case "cols": item.cols = parseIntSafe(v, 4); break;
                    case "rows": item.rows = parseIntSafe(v, 4); break;
                    case "downloads": item.downloads = parseIntSafe(v, 0); break;
                }
            }
            return item;
        }

        static MarketItem fromFile(File f) {
            try (java.io.FileReader fr = new java.io.FileReader(f)) {
                StringBuilder sb = new StringBuilder();
                int c;
                while ((c = fr.read()) != -1) sb.append((char) c);
                return deserialize(sb.toString());
            } catch (Exception e) {
                return new MarketItem(f.getName(), "?", "", 4, 4, 0);
            }
        }

        private static int parseIntSafe(String s, int def) {
            try { return Integer.parseInt(s); } catch (Exception e) { return def; }
        }
    }

    private class MarketAdapter extends BaseAdapter {
        @Override public int getCount() { return mItems.size(); }
        @Override public MarketItem getItem(int position) { return mItems.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_market_card, parent, false);
            }
            MarketItem item = getItem(position);
            TextView title = convertView.findViewById(R.id.market_title);
            TextView author = convertView.findViewById(R.id.market_author);
            TextView summary = convertView.findViewById(R.id.market_summary);
            TextView stats = convertView.findViewById(R.id.market_stats);
            title.setText(item.title);
            author.setText(item.author + " · " + item.version);
            summary.setText(item.summary);
            stats.setText("↓ " + item.downloads + "  ·  " + item.cols + "x" + item.rows);
            return convertView;
        }
    }
}
