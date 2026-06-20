package com.example.music_mp3;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.gson.Gson;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 vpAlbum;
    private List<Album> albumDataList = new ArrayList<>();
    private AlbumPagerAdapter pagerAdapter;

    private MediaPlayer mediaPlayer;
    private final Handler handler = new Handler();
    private List<PageState> pageStates = new ArrayList<>();

    private int currentPageIndex = 0;
    private float touchStartY = 0;
    private long lastTipTime = 0;

    private OkHttpClient okHttpClient;
    private static final String SERVER_IP = "192.168.0.101";
    private static final int SERVER_PORT = 8081;
    private final Gson gson = new Gson();

    // 【已删除】广播 Action 和 Receiver 变量，彻底解决版本兼容报错

    class PageState {
        int position;
        RecyclerView rvSongWheel;
        SongWheelAdapter wheelAdapter;
        TextView tvSongName, tvSinger, tv_current_time, tv_total_time;
        TextView tvCity, tvTemp, tvWeatherText;
        SeekBar seekBar;
        ImageButton btnPlay, btnPrev, btnNext;
        List<Music> musicList;
        LinearLayoutManager layoutManager;
        PagerSnapHelper snapHelper;
        int currentAbsPos;
        int currentIndex;
        boolean isPlaying;
        int loadIndex;
        String[] currentAlbumBvArr;
        Runnable progressRunnable;
        boolean isListenersAttached = false;
        boolean isDataLoaded = false;

        ImageView ivPlayMode;
        int playMode = 0;

        PageState(int pos) {
            this.position = pos;
            this.musicList = new ArrayList<>();
            Music temp = new Music();
            temp.setName("加载中...");
            temp.setSinger("等待B站数据");
            musicList.add(temp);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 【已删除】广播注册代码

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("冬谷Walkman");
        }

        ImageButton btnInfo = findViewById(R.id.btn_info);
        btnInfo.setOnClickListener(v -> {
            String url = "https://wiki.biligame.com/whmx/%E9%A6%96%E9%A1%B5";
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "未找到可用的浏览器", Toast.LENGTH_SHORT).show();
            }
        });

        vpAlbum = findViewById(R.id.vp_album);
        mediaPlayer = new MediaPlayer();

        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        fetchAlbumDataFromServer();
    }

    private void fetchAlbumDataFromServer() {
        String url = "http://" + SERVER_IP + ":" + SERVER_PORT + "/api/library/getAll";
        Request request = new Request.Builder().url(url).get().build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "连接后端失败，请检查网络或后端服务", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) return;
                String jsonStr = response.body().string();
                try {
                    JSONObject root = new JSONObject(jsonStr);
                    JSONObject dataObj = root.getJSONObject("data");

                    albumDataList.clear();

                    JSONArray albumNames = dataObj.names();
                    if (albumNames != null) {
                        for (int i = 0; i < albumNames.length(); i++) {
                            String albumName = albumNames.getString(i);
                            JSONArray bvArray = dataObj.getJSONArray(albumName);

                            String[] bvs = new String[bvArray.length()];
                            for (int j = 0; j < bvArray.length(); j++) {
                                bvs[j] = bvArray.getString(j);
                            }

                            Album album = new Album(bvs);
                            album.setAlbumName(albumName);
                            albumDataList.add(album);
                        }
                    }

                    runOnUiThread(() -> {
                        initPagerAndListeners();
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "解析后端数据失败", Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void initPagerAndListeners() {
        vpAlbum.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        RecyclerView recyclerView = (RecyclerView) vpAlbum.getChildAt(0);
        if (recyclerView != null) {
            recyclerView.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }

        pagerAdapter = new AlbumPagerAdapter(this, albumDataList, this::initSingleAlbumPage);
        vpAlbum.setAdapter(pagerAdapter);
        vpAlbum.setOrientation(ViewPager2.ORIENTATION_VERTICAL);

        vpAlbum.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPageIndex = position;
                stopMediaPlayer();
            }
        });

        RecyclerView innerRv = (RecyclerView) vpAlbum.getChildAt(0);
        innerRv.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    touchStartY = event.getY();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float touchEndY = event.getY();
                    float deltaY = touchEndY - touchStartY;
                    long currentTime = System.currentTimeMillis();
                    if (Math.abs(deltaY) > 100 && (currentTime - lastTipTime > 1000)) {
                        if (currentPageIndex == 0 && deltaY > 0) {
                            Toast.makeText(MainActivity.this, "已经到顶了~", Toast.LENGTH_SHORT).show();
                            lastTipTime = currentTime;
                        } else if (currentPageIndex == albumDataList.size() - 1 && deltaY < 0) {
                            Toast.makeText(MainActivity.this, "已经到底了~", Toast.LENGTH_SHORT).show();
                            lastTipTime = currentTime;
                        }
                    }
                    break;
            }
            return false;
        });
    }

    private void initSingleAlbumPage(View pageRoot, Album album, int pagePos) {
        PageState state;
        if (pagePos >= pageStates.size()) {
            state = new PageState(pagePos);
            pageStates.add(state);
        } else {
            state = pageStates.get(pagePos);
        }

        state.rvSongWheel = pageRoot.findViewById(R.id.rvSongWheel);
        state.tvSongName = pageRoot.findViewById(R.id.tvSongName);
        state.tvSinger = pageRoot.findViewById(R.id.tvSinger);
        state.seekBar = pageRoot.findViewById(R.id.seekBar);
        state.btnPlay = pageRoot.findViewById(R.id.btnPlay);
        state.btnPrev = pageRoot.findViewById(R.id.btnPrev);
        state.btnNext = pageRoot.findViewById(R.id.btnNext);
        state.tv_current_time = pageRoot.findViewById(R.id.tv_current_time);
        state.tv_total_time = pageRoot.findViewById(R.id.tv_total_time);
        state.tvCity = pageRoot.findViewById(R.id.tv_city);
        state.tvTemp = pageRoot.findViewById(R.id.tv_temp);
        state.tvWeatherText = pageRoot.findViewById(R.id.tv_weather_text);
        state.ivPlayMode = pageRoot.findViewById(R.id.iv_play_mode);

        ImageButton btnAddSong = pageRoot.findViewById(R.id.btn_add_song);
        btnAddSong.setOnClickListener(v -> {
            EditText input = new EditText(MainActivity.this);
            input.setHint("请输入B站BV号 (如 BV1xx)");

            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                    .setTitle("添加歌曲到当前轮盘")
                    .setView(input)
                    .setPositiveButton("添加", (dialog, which) -> {
                        final String bv = input.getText().toString().trim();

                        if (bv.isEmpty() || !bv.startsWith("BV")) {
                            Toast.makeText(MainActivity.this, "BV号格式错误", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int targetIndex = currentPageIndex;
                        final Album targetAlbum = albumDataList.get(targetIndex);

                        String url = "http://" + SERVER_IP + ":" + SERVER_PORT + "/api/library/addSong";
                        JSONObject jsonBody = new JSONObject();
                        try {
                            jsonBody.put("albumName", targetAlbum.albumName);
                            jsonBody.put("bv", bv);
                        } catch (Exception e) { e.printStackTrace(); }

                        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
                        Request request = new Request.Builder().url(url).post(body).build();

                        okHttpClient.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show());
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                runOnUiThread(() -> {
                                    String[] oldBvs = targetAlbum.bvList;
                                    String[] newBvs = new String[oldBvs.length + 1];
                                    System.arraycopy(oldBvs, 0, newBvs, 0, oldBvs.length);
                                    newBvs[oldBvs.length] = bv;
                                    targetAlbum.bvList = newBvs;

                                    PageState targetState = pageStates.get(targetIndex);
                                    targetState.currentAlbumBvArr = targetAlbum.bvList;

                                    targetState.isDataLoaded = false;
                                    targetState.musicList.clear();

                                    Music temp = new Music();
                                    temp.setName("加载中...");
                                    temp.setSinger("等待B站数据");
                                    targetState.musicList.add(temp);

                                    if (targetState.wheelAdapter != null) {
                                        targetState.wheelAdapter.notifyDataSetChanged();
                                    }

                                    targetState.loadIndex = 0;
                                    loadNextBv(targetState);

                                    Toast.makeText(MainActivity.this, "已添加并保存到数据库", Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });

        if (state.layoutManager == null) {
            state.layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
            state.rvSongWheel.setLayoutManager(state.layoutManager);
            state.snapHelper = new PagerSnapHelper();
            state.snapHelper.attachToRecyclerView(state.rvSongWheel);
            state.rvSongWheel.addOnScrollListener(new ScaleScrollListener(state.layoutManager));
        }

        if (!state.isListenersAttached) {
            state.isListenersAttached = true;

            state.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}
                @Override public void onStartTrackingTouch(SeekBar seekBar) { handler.removeCallbacks(state.progressRunnable); }
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.seekTo(seekBar.getProgress());
                        handler.post(state.progressRunnable);
                    }
                }
            });

            state.rvSongWheel.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        View snapView = state.snapHelper.findSnapView(state.layoutManager);
                        if (snapView == null || state.musicList.isEmpty()) return;
                        int pos = state.layoutManager.getPosition(snapView);
                        state.currentAbsPos = pos;
                        int realIdx = pos % state.musicList.size();
                        if (realIdx != state.currentIndex) {
                            stopMediaPlayer();
                            state.currentIndex = realIdx;
                            refreshUI(state, state.musicList.get(state.currentIndex));
                            if (state.isPlaying) {
                                playCurrentSelectedMusic(state);
                            }
                        }
                    }
                }
            });

            state.btnPlay.setOnClickListener(v -> {
                state.isPlaying = !state.isPlaying;
                if (state.isPlaying) {
                    state.btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                    playCurrentSelectedMusic(state);
                } else {
                    state.btnPlay.setImageResource(android.R.drawable.ic_media_play);
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        handler.removeCallbacks(state.progressRunnable);
                    }
                }
            });

            state.btnPrev.setOnClickListener(v -> {
                if (state.musicList.isEmpty()) return;
                View snapView = state.snapHelper.findSnapView(state.layoutManager);
                if (snapView != null) {
                    RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) snapView.getLayoutParams();
                    int itemWidth = snapView.getWidth() + params.leftMargin + params.rightMargin;
                    state.rvSongWheel.smoothScrollBy(-itemWidth, 0);
                }
            });

            state.btnNext.setOnClickListener(v -> {
                if (state.musicList.isEmpty()) return;
                View snapView = state.snapHelper.findSnapView(state.layoutManager);
                if (snapView != null) {
                    RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) snapView.getLayoutParams();
                    int itemWidth = snapView.getWidth() + params.leftMargin + params.rightMargin;
                    state.rvSongWheel.smoothScrollBy(itemWidth, 0);
                }
            });

            state.ivPlayMode.setOnClickListener(v -> {
                state.playMode = (state.playMode + 1) % 3;
                updatePlayModeIcon(state);
            });
        }

        if (state.wheelAdapter == null) {
            state.progressRunnable = new Runnable() {
                @Override public void run() {
                    if (mediaPlayer != null && mediaPlayer.isPlaying() && state.isPlaying) {
                        int currPos = mediaPlayer.getCurrentPosition();
                        int totalDur = mediaPlayer.getDuration();
                        state.seekBar.setProgress(currPos);
                        state.tv_current_time.setText(timeFormat(currPos));
                        state.tv_total_time.setText(timeFormat(totalDur));
                    }
                    handler.postDelayed(this, 50);
                }
            };

            // 传入第三个参数：长按监听器
            state.wheelAdapter = new SongWheelAdapter(state.musicList, realPos -> {
                if (state.musicList.isEmpty()) return;
                int realIdx = realPos % state.musicList.size();
                if (realIdx != state.currentIndex) {
                    stopMediaPlayer();
                    state.currentIndex = realIdx;
                    refreshUI(state, state.musicList.get(state.currentIndex));
                }
            }, (pos, music) -> {
                // 长按回调，弹出删除对话框
                showDeleteDialog(state, music);
            });
            state.rvSongWheel.setAdapter(state.wheelAdapter);
        } else {
            state.wheelAdapter.notifyDataSetChanged();
        }

        int totalItemCount = state.musicList.size() * 100;
        int midBase = (totalItemCount / 2 / state.musicList.size()) * state.musicList.size();
        state.currentAbsPos = midBase;

        state.rvSongWheel.post(() -> {
            state.rvSongWheel.scrollToPosition(state.currentAbsPos);
            state.rvSongWheel.post(() -> {
                View snapView = state.layoutManager.findViewByPosition(state.currentAbsPos);
                if (snapView != null) {
                    RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) snapView.getLayoutParams();
                    int itemWidth = snapView.getWidth() + params.leftMargin + params.rightMargin;

                    int recyclerWidth = state.rvSongWheel.getWidth();
                    int viewCenter = snapView.getLeft() + itemWidth / 2;
                    int recyclerCenter = recyclerWidth / 2;
                    int dx = viewCenter - recyclerCenter;

                    state.rvSongWheel.smoothScrollBy(dx, 0);
                }
            });
        });

        state.currentIndex = state.currentAbsPos % state.musicList.size();
        refreshUI(state, state.musicList.get(state.currentIndex));
        updatePlayModeIcon(state);

        if (!state.isDataLoaded) {
            state.isDataLoaded = true;
            state.currentAlbumBvArr = album.bvList;
            state.loadIndex = 0;
            loadNextBv(state);

            requestLocationAndWeather(state);

            View weatherLayout = pageRoot.findViewById(R.id.weather_layout);
            if (weatherLayout != null) {
                weatherLayout.setOnClickListener(v -> showHistoryDialog());
            }
        }
    }

    // ================= 删除歌曲相关逻辑 =================

    private void showDeleteDialog(PageState state, Music music) {
        new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                .setTitle("删除歌曲")
                .setMessage("确定要删除《" + music.getName() + "》吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    deleteSongFromServer(state, music);
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteSongFromServer(PageState state, Music music) {
        String albumName = albumDataList.get(state.position).albumName;
        String bv = music.getBv();

        String url = "http://" + SERVER_IP + ":" + SERVER_PORT + "/api/library/deleteSong?albumName="
                + Uri.encode(albumName) + "&bv=" + Uri.encode(bv);

        Request request = new Request.Builder().url(url).get().build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "网络请求失败", Toast.LENGTH_SHORT).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                runOnUiThread(() -> {
                    // 1. 从内存的 musicList 中移除
                    state.musicList.remove(music);
                    if (state.wheelAdapter != null) {
                        state.wheelAdapter.notifyDataSetChanged();
                    }

                    // 2. 同步更新 Album 的 bvList 数组
                    Album currentAlbum = albumDataList.get(state.position);
                    List<String> tempBvList = new ArrayList<>(Arrays.asList(currentAlbum.bvList));
                    tempBvList.remove(bv);
                    currentAlbum.bvList = tempBvList.toArray(new String[0]);
                    state.currentAlbumBvArr = currentAlbum.bvList;

                    // 3. 如果删光了，加个占位符防止崩溃
                    if (state.musicList.isEmpty()) {
                        Music temp = new Music();
                        temp.setName("暂无歌曲");
                        temp.setSinger("请添加");
                        state.musicList.add(temp);
                        if (state.wheelAdapter != null) state.wheelAdapter.notifyDataSetChanged();
                    } else {
                        if (state.currentIndex >= state.musicList.size()) {
                            state.currentIndex = 0;
                        }
                        refreshUI(state, state.musicList.get(state.currentIndex));
                    }

                    // 4. 【修改】直接弹出提示，简单高效，彻底告别广播报错！
                    Toast.makeText(MainActivity.this, "歌曲《" + music.getName() + "》已删除", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    // ================= 实时定位与天气逻辑 =================

    private void requestLocationAndWeather(PageState state) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }

            if (location != null) {
                String lngLat = String.format(Locale.US, "%.2f:%.2f",
                        location.getLongitude(), location.getLatitude());
                requestWeather(state, lngLat, "秦皇岛");
                return;
            }
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }

        requestWeather(state, "auto:ip", "秦皇岛");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!pageStates.isEmpty()) {
                    requestLocationAndWeather(pageStates.get(vpAlbum.getCurrentItem()));
                }
            } else {
                if (!pageStates.isEmpty()) {
                    requestWeather(pageStates.get(vpAlbum.getCurrentItem()), "auto:ip", "秦皇岛");
                }
            }
        }
    }

    // ================= 原有逻辑 =================

    private String timeFormat(long ms) {
        if (ms <= 0) return "00:00";
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    private void loadNextBv(PageState state) {
        if (state.loadIndex >= state.currentAlbumBvArr.length) {
            runOnUiThread(() -> {
                if (state.musicList.size() > 1) {
                    Music firstItem = state.musicList.get(0);
                    if ("加载中...".equals(firstItem.getName())) {
                        state.musicList.remove(0);
                    }
                }
                if (state.wheelAdapter != null) state.wheelAdapter.notifyDataSetChanged();

                if (!state.musicList.isEmpty()) {
                    if (state.currentIndex >= state.musicList.size()) {
                        state.currentIndex = 0;
                    }
                    refreshUI(state, state.musicList.get(state.currentIndex));
                }

                state.rvSongWheel.postDelayed(() -> {
                    View snapView = state.snapHelper.findSnapView(state.layoutManager);
                    if (snapView != null) {
                        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) snapView.getLayoutParams();
                        int itemWidth = snapView.getWidth() + params.leftMargin + params.rightMargin;
                        state.rvSongWheel.smoothScrollBy(itemWidth / 3, 0);
                    }
                }, 200);
            });
            return;
        }
        String bv = state.currentAlbumBvArr[state.loadIndex];
        loadSingleBv(state, bv, () -> {
            state.loadIndex++;
            loadNextBv(state);
        });
    }

    private void loadSingleBv(PageState state, String bv, Runnable finishCallback) {
        String reqUrl = String.format("http://%s:%d/bili/getVideoInfo?bv=%s", SERVER_IP, SERVER_PORT, bv);
        Request request = new Request.Builder().url(reqUrl).get().build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> { if (state.tvCity != null) state.tvCity.setText("BV获取失败"); });
                finishCallback.run();
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null || !response.isSuccessful()) {
                    runOnUiThread(() -> { if (state.tvCity != null) state.tvCity.setText("接口异常"); });
                    finishCallback.run();
                    return;
                }
                String json = response.body().string();
                BiliVideoBean bean = gson.fromJson(json, BiliVideoBean.class);
                runOnUiThread(() -> {
                    Music newMusic = new Music();
                    newMusic.setBv(bv);
                    newMusic.setName(bean.getTitle());
                    newMusic.setSinger(bean.getUpName());
                    newMusic.setCoverUrl(bean.getCoverUrl());
                    newMusic.setUrl(bean.getAudioProxyUrl());
                    state.musicList.add(newMusic);
                    if (state.wheelAdapter != null) state.wheelAdapter.notifyDataSetChanged();
                });
                finishCallback.run();
            }
        });
    }

    private void stopMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.reset();
        }
        for (PageState state : pageStates) {
            state.isPlaying = false;
            handler.removeCallbacks(state.progressRunnable);
            runOnUiThread(() -> {
                if (state.btnPlay != null) state.btnPlay.setImageResource(android.R.drawable.ic_media_play);
                if (state.seekBar != null) state.seekBar.setProgress(0);
                if (state.tv_current_time != null) state.tv_current_time.setText("00:00");
                if (state.tv_total_time != null) state.tv_total_time.setText("00:00");
            });
        }
    }

    private void playCurrentSelectedMusic(PageState state) {
        if (state.musicList.isEmpty()) return;
        Music selectMusic = state.musicList.get(state.currentIndex);
        String audioUrl = selectMusic.getUrl();
        if (audioUrl == null || audioUrl.isEmpty()) return;
        try {
            mediaPlayer.reset();
            setupCompletionListener(state);

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                state.isPlaying = true;
                savePlayHistory(selectMusic);

                runOnUiThread(() -> {
                    if (state.btnPlay != null) state.btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                    if (state.seekBar != null) state.seekBar.setMax(mp.getDuration());
                    if (state.tv_total_time != null) state.tv_total_time.setText(timeFormat(mp.getDuration()));
                });
                handler.post(state.progressRunnable);
            });
            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestWeather(PageState state, String locationParam, String fallbackCity) {
        String newKey = "SqkjmFZFn9Cxvwefy";
        String url = "https://api.seniverse.com/v3/weather/now.json?key=" + newKey + "&location=" + locationParam + "&language=zh-Hans&unit=c";

        Request request = new Request.Builder().url(url).get().build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                requestWeather(state, fallbackCity, fallbackCity);
            }
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) return;
                String jsonStr = response.body().string();
                response.close();
                try {
                    JSONObject root = new JSONObject(jsonStr);

                    if (root.has("error") || !root.has("results")) {
                        if (!locationParam.equals(fallbackCity)) {
                            requestWeather(state, fallbackCity, fallbackCity);
                        } else {
                            throw new JSONException("API错误");
                        }
                        return;
                    }

                    JSONObject result = root.getJSONArray("results").getJSONObject(0);
                    JSONObject now = result.getJSONObject("now");
                    String city = result.getJSONObject("location").getString("name");
                    String temp = now.getString("temperature") + "℃";
                    String weatherDesc = now.getString("text");

                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (state.tvCity != null) state.tvCity.setText(city);
                        if (state.tvTemp != null) state.tvTemp.setText(temp);
                        if (state.tvWeatherText != null) state.tvWeatherText.setText(weatherDesc);
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (state.tvCity != null) state.tvCity.setText(fallbackCity);
                        if (state.tvTemp != null) state.tvTemp.setText("--");
                        if (state.tvWeatherText != null) state.tvWeatherText.setText("天气服务暂不可用");
                    });
                }
            }
        });
    }

    private void refreshUI(PageState state, Music music) {
        if (state.tvSongName != null) state.tvSongName.setText(music.getName());
        if (state.tvSinger != null) state.tvSinger.setText(music.getSinger());
    }

    private void updatePlayModeIcon(PageState state) {
        if (state.ivPlayMode == null) return;
        switch (state.playMode) {
            case 0:
                state.ivPlayMode.setImageResource(R.drawable.ic_mode_loop);
                break;
            case 1:
                state.ivPlayMode.setImageResource(R.drawable.ic_mode_shuffle);
                break;
            case 2:
                state.ivPlayMode.setImageResource(R.drawable.ic_mode_single);
                break;
        }
    }

    private void setupCompletionListener(PageState state) {
        mediaPlayer.setOnCompletionListener(mp -> {
            if (state.playMode == 2) {
                playCurrentSelectedMusic(state);
            } else {
                playNextSongByMode(state);
            }
        });
    }

    private void playNextSongByMode(PageState state) {
        if (state.musicList.isEmpty()) return;

        int nextIndex;
        int nextAbsPos;

        if (state.playMode == 1) {
            if (state.musicList.size() == 1) {
                nextIndex = 0;
            } else {
                do {
                    nextIndex = (int) (Math.random() * state.musicList.size());
                } while (nextIndex == state.currentIndex);
            }
            int currentRemainder = state.currentAbsPos % state.musicList.size();
            if (currentRemainder < 0) currentRemainder += state.musicList.size();

            int diff = nextIndex - currentRemainder;
            if (diff <= 0) diff += state.musicList.size();

            int randomCircles = (int) (Math.random() * 2) + 1;
            nextAbsPos = state.currentAbsPos + diff + (randomCircles * state.musicList.size());
        } else {
            nextIndex = (state.currentIndex + 1) % state.musicList.size();
            nextAbsPos = state.currentAbsPos + 1;
        }

        state.currentIndex = nextIndex;
        state.currentAbsPos = nextAbsPos;
        refreshUI(state, state.musicList.get(state.currentIndex));

        state.rvSongWheel.scrollToPosition(state.currentAbsPos);

        state.rvSongWheel.postDelayed(() -> {
            View snapView = state.layoutManager.findViewByPosition(state.currentAbsPos);
            if (snapView != null) {
                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) snapView.getLayoutParams();
                int itemWidth = snapView.getWidth() + params.leftMargin + params.rightMargin;

                int recyclerWidth = state.rvSongWheel.getWidth();
                int viewCenter = snapView.getLeft() + itemWidth / 2;
                int recyclerCenter = recyclerWidth / 2;
                int dx = viewCenter - recyclerCenter;

                state.rvSongWheel.smoothScrollBy(dx, 0);
            }
        }, 100);

        playCurrentSelectedMusic(state);
    }

    private void savePlayHistory(Music music) {
        if (music == null || music.getBv() == null) return;
        String url = "http://" + SERVER_IP + ":" + SERVER_PORT + "/api/record/addHistory";
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("bv", music.getBv());
            jsonBody.put("title", music.getName());
            jsonBody.put("upName", music.getSinger());
            jsonBody.put("coverUrl", music.getCoverUrl());
            jsonBody.put("audioUrl", music.getUrl());
        } catch (Exception e) { e.printStackTrace(); return; }

        RequestBody body = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(body).build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) {}
        });
    }

    private void showHistoryDialog() {
        String url = "http://" + SERVER_IP + ":" + SERVER_PORT + "/api/record/getList?type=1";
        Request request = new Request.Builder().url(url).get().build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) return;
                String jsonStr = response.body().string();
                try {
                    JSONObject root = new JSONObject(jsonStr);
                    JSONArray dataArray = root.getJSONArray("data");

                    androidx.appcompat.app.AlertDialog.Builder builder =
                            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this);
                    builder.setTitle(" 播放历史记录");

                    if (dataArray.length() == 0) {
                        builder.setMessage("暂无播放记录");
                        builder.setPositiveButton("关闭", null);
                        runOnUiThread(builder::show);
                        return;
                    }

                    final String[] historyItems = new String[dataArray.length()];
                    final String[] historyBVs = new String[dataArray.length()];

                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject item = dataArray.getJSONObject(i);
                        String title = item.getString("title");
                        String upName = item.getString("upName");
                        String bv = item.getString("bv");
                        String time = item.getString("createTime").substring(0, 16);

                        historyItems[i] = (i + 1) + ". " + title + "\n   - " + upName + "\n    " + time;
                        historyBVs[i] = bv;
                    }

                    builder.setItems(historyItems, (dialog, which) -> {
                        String selectedBV = historyBVs[which];
                        new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                                .setTitle("删除记录")
                                .setMessage("要删除这条播放记录吗？")
                                .setPositiveButton("删除", (d, w) -> deleteHistory(selectedBV))
                                .setNegativeButton("取消", null)
                                .show();
                    });

                    builder.setNeutralButton("清空全部", (d, w) -> {
                        new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                                .setTitle("确认清空")
                                .setMessage("确定要清空所有播放历史吗？")
                                .setPositiveButton("确定", (d2, w2) -> clearAllHistory())
                                .setNegativeButton("取消", null)
                                .show();
                    });

                    builder.setPositiveButton("关闭", null);
                    runOnUiThread(builder::show);

                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    private void deleteHistory(String bv) {
        String url = "http://" + SERVER_IP + ":" + SERVER_PORT + "/api/record/deleteHistory?bv=" + bv;
        Request request = new Request.Builder().url(url).delete().build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                    showHistoryDialog();
                });
            }
        });
    }

    private void clearAllHistory() {
        String url = "http://" + SERVER_IP + ":" + SERVER_PORT + "/api/record/clearHistory";
        Request request = new Request.Builder().url(url).delete().build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "已清空所有历史", Toast.LENGTH_SHORT).show();
                    showHistoryDialog();
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // 【已删除】广播注销代码

        handler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}