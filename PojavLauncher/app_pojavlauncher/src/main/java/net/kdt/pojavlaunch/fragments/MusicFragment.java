package net.kdt.pojavlaunch.fragments;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MusicFragment extends Fragment {

    private static final String TAG = "MusicFragment";

    private final List<SongItem> mSongs = new ArrayList<>();
    private SongAdapter mAdapter;

    private ListView mMusicList;
    private TextView mNowPlayingText;
    private TextView mNowPlayingName;
    private SeekBar mProgressBar;
    private ImageButton mPrevButton, mPlayButton, mNextButton;

    private MediaPlayer mPlayer;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private int mCurrentIndex = -1;
    private final Runnable mProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (mPlayer != null) {
                mProgressBar.setMax(mPlayer.getDuration());
                mProgressBar.setProgress(mPlayer.getCurrentPosition());
            }
            mHandler.postDelayed(this, 500);
        }
    };

    private final ActivityResultLauncher<String[]> mAudioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    granted -> {
                        if (granted.containsValue(Boolean.TRUE)) scanSongs();
                        else Toast.makeText(requireContext(), R.string.music_permission_denied, Toast.LENGTH_SHORT).show();
                    });

    private final ActivityResultLauncher<String[]> mOpenAudioLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenMultipleDocuments(),
                    uris -> {
                        if (uris == null || uris.isEmpty()) return;
                        importSongs(uris);
                    });

    public MusicFragment() {
        super(R.layout.fragment_music);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        mMusicList = view.findViewById(R.id.music_list);
        mNowPlayingText = view.findViewById(R.id.music_now_playing_text);
        mNowPlayingName = view.findViewById(R.id.music_now_playing_name);
        mProgressBar = view.findViewById(R.id.music_progress);
        mPrevButton = view.findViewById(R.id.music_prev_button);
        mPlayButton = view.findViewById(R.id.music_play_button);
        mNextButton = view.findViewById(R.id.music_next_button);
        ImageButton importButton = view.findViewById(R.id.music_import_button);

        mAdapter = new SongAdapter(requireContext(), mSongs);
        mMusicList.setAdapter(mAdapter);
        mMusicList.setOnItemClickListener((parent, view1, position, id) -> playSong(position));

        importButton.setOnClickListener(v -> requestAudioPermission());
        mPrevButton.setOnClickListener(v -> playSong(prevIndex()));
        mNextButton.setOnClickListener(v -> playSong(nextIndex()));
        mPlayButton.setOnClickListener(v -> togglePlay());

        mProgressBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mPlayer != null) mPlayer.seekTo(progress);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        scanSongs();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mHandler.removeCallbacks(mProgressRunnable);
        releasePlayer();
    }

    /* ============================= Permission & scan ============================= */

    private void requestAudioPermission() {
        Context context = getContext();
        if (context == null) return;
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                mAudioPermissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_AUDIO});
                return;
            }
        } else if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                mAudioPermissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
                return;
            }
        }
        mOpenAudioLauncher.launch(new String[]{"audio/*"});
    }

    private void scanSongs() {
        mSongs.clear();
        scanMediaStore();
        scanLocalImports();
        mAdapter.notifyDataSetChanged();
        updatePlayStatus();
    }

    private void scanMediaStore() {
        try {
            ContentResolver resolver = requireContext().getContentResolver();
            String[] projection = {MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA};
            Cursor cursor = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection, MediaStore.Audio.Media.IS_MUSIC + " != 0", null, null);
            if (cursor != null) {
                int titleIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
                int dataIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
                while (cursor.moveToNext()) {
                    String title = cursor.getString(titleIndex);
                    String path = cursor.getString(dataIndex);
                    if (path != null && new File(path).exists()) {
                        mSongs.add(new SongItem(title == null ? "未知歌曲" : title, path));
                    }
                }
                cursor.close();
            }
        } catch (Exception ignored) {
        }
    }

    private void scanLocalImports() {
        File dir = new File(requireContext().getFilesDir(), "music");
        if (!dir.exists()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile() && isAudioFile(f.getName())) {
                mSongs.add(new SongItem(f.getName(), f.getAbsolutePath()));
            }
        }
    }

    private boolean isAudioFile(String name) {
        String lower = name.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".m4a") || lower.endsWith(".flac");
    }

    private void importSongs(List<Uri> uris) {
        Context context = requireContext();
        File dir = new File(context.getFilesDir(), "music");
        if (!dir.exists()) dir.mkdirs();
        int imported = 0;
        for (Uri uri : uris) {
            try (InputStream in = context.getContentResolver().openInputStream(uri)) {
                if (in == null) continue;
                String name = queryDisplayName(uri);
                File outFile = new File(dir, sanitizeFileName(name));
                int counter = 1;
                while (outFile.exists()) {
                    String base = name.substring(0, name.lastIndexOf('.'));
                    String ext = name.substring(name.lastIndexOf('.'));
                    outFile = new File(dir, base + "_" + counter + ext);
                    counter++;
                }
                try (OutputStream out = new FileOutputStream(outFile)) {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) out.write(buffer, 0, read);
                }
                imported++;
            } catch (Exception ignored) {
            }
        }
        if (imported > 0) {
            Toast.makeText(context, getString(R.string.music_imported, imported + " 首"), Toast.LENGTH_SHORT).show();
            scanSongs();
        } else {
            Toast.makeText(context, R.string.music_import_failed, Toast.LENGTH_SHORT).show();
        }
    }

    private String queryDisplayName(Uri uri) {
        try {
            Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                int index = cursor.getColumnIndexOrThrow("_display_name");
                if (cursor.moveToFirst()) return cursor.getString(index);
            }
        } catch (Exception ignored) {
        }
        return "audio_" + System.currentTimeMillis() + ".mp3";
    }

    private String sanitizeFileName(String name) {
        String sanitized = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        return sanitized.isEmpty() ? "audio.mp3" : sanitized;
    }

    /* ============================= Playback ============================= */

    private void playSong(int index) {
        if (index < 0 || index >= mSongs.size()) return;
        mCurrentIndex = index;
        releasePlayer();
        try {
            mPlayer = new MediaPlayer();
            mPlayer.setDataSource(mSongs.get(index).path);
            mPlayer.prepare();
            mPlayer.start();
            mPlayer.setOnCompletionListener(mp -> playSong(nextIndex()));
            mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
            mNowPlayingName.setText(mSongs.get(index).title);
            mNowPlayingText.setText(getString(R.string.music_playing, mSongs.get(index).title));
            mHandler.removeCallbacks(mProgressRunnable);
            mHandler.post(mProgressRunnable);
        } catch (Exception e) {
            releasePlayer();
            mCurrentIndex = -1;
            Toast.makeText(requireContext(), "无法播放该音乐：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        updatePlayStatus();
    }

    private void togglePlay() {
        if (mPlayer == null) return;
        if (mPlayer.isPlaying()) {
            mPlayer.pause();
            mPlayButton.setImageResource(android.R.drawable.ic_media_play);
        } else {
            mPlayer.start();
            mPlayButton.setImageResource(android.R.drawable.ic_media_pause);
        }
    }

    private int nextIndex() {
        if (mSongs.isEmpty()) return -1;
        return (mCurrentIndex + 1) % mSongs.size();
    }

    private int prevIndex() {
        if (mSongs.isEmpty()) return -1;
        if (mCurrentIndex <= 0) return mSongs.size() - 1;
        return mCurrentIndex - 1;
    }

    private void updatePlayStatus() {
        if (mSongs.isEmpty()) {
            mNowPlayingText.setText(R.string.music_empty);
            mNowPlayingName.setText("");
            mPlayButton.setEnabled(false);
        } else {
            mPlayButton.setEnabled(true);
        }
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            try { mPlayer.stop(); } catch (Exception ignored) {}
            mPlayer.release();
            mPlayer = null;
        }
    }

    private static class SongItem {
        final String title;
        final String path;

        SongItem(String title, String path) {
            this.title = title;
            this.path = path;
        }
    }

    private static class SongAdapter extends BaseAdapter {
        private final Context mContext;
        private final List<SongItem> mItems;

        SongAdapter(Context context, List<SongItem> items) {
            mContext = context;
            mItems = items;
        }

        @Override public int getCount() { return mItems.size(); }

        @Override public SongItem getItem(int position) { return mItems.get(position); }

        @Override public long getItemId(int position) { return position; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.item_music, parent, false);
            }
            SongItem item = getItem(position);
            TextView titleView = convertView.findViewById(R.id.music_item_title);
            titleView.setText(item.title);
            return convertView;
        }
    }
}
