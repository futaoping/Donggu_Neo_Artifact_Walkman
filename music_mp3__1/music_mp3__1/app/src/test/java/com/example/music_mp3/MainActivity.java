package com.example.music_mp3;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvSongWheel;
    private SongWheelAdapter wheelAdapter;
    private TextView tvSongName, tvSinger;
    private SeekBar seekBar;
    private ImageButton btnPlay, btnPrev, btnNext;
    private MediaPlayer mediaPlayer;
    private List<Music> musicList;
    private int currentIndex = 0;
    private final Handler handler = new Handler();
    private boolean isPlaying = false;
    private LinearLayoutManager layoutManager;
    private PagerSnapHelper snapHelper;

    private int currentAbsPos;
    private boolean scrollLock = false;
    private boolean switchingSong = false;

    // 进度条定时刷新任务
    private Runnable progressRunnable;

    // 天气控件
    private TextView tvCity, tvTemp, tvWeatherText;
    // 新增播放时长控件
    private TextView tv_current_time, tv_total_time;
    private OkHttpClient okHttpClient;

    // ===================== 后端局域网IP+端口 =====================
    private static final String SERVER_IP = "192.168.0.101";
    private static final int SERVER_PORT = 8081;
    private final Gson gson = new Gson();
    // ==========================================================

    // 需要加载的BV数组，串行逐个加载
    private final String[] bvArr = {
            "BV1evEY6AEuY",
            "BV1H4L86cEq9",
            "BV1ynGa6wEVs"
    };
    private int loadIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        rvSongWheel = findViewById(R.id.rvSongWheel);
        tvSongName = findViewById(R.id.tvSongName);
        tvSinger = findViewById(R.id.tvSinger);
        seekBar = findViewById(R.id.seekBar);
        btnPlay = findViewById(R.id.btnPlay);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        mediaPlayer = new MediaPlayer();

        tvCity = findViewById(R.id.tv_city);
        tvTemp = findViewById(R.id.tv_temp);
        tvWeatherText = findViewById(R.id.tv_weather_text);
        // 绑定时长文本控件
        tv_current_time = findViewById(R.id.tv_current_time);
        tv_total_time = findViewById(R.id.tv_total_time);

        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();

        layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvSongWheel.setLayoutManager(layoutManager);
        snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(rvSongWheel);
        rvSongWheel.addOnScrollListener(new ScaleScrollListener(layoutManager));

        // 进度条拖拽监听
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(progressRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                    handler.post(progressRunnable);
                }
            }
        });

        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int currPos = mediaPlayer.getCurrentPosition();
                    int totalDur = mediaPlayer.getDuration();
                    seekBar.setProgress(currPos);
                    // 实时更新时分秒
                    tv_current_time.setText(timeFormat(currPos));
                    tv_total_time.setText(timeFormat(totalDur));
                }
                handler.postDelayed(this, 50);
            }
        };

        rvSongWheel.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (switchingSong) return;

                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    scrollLock = false;
                    View snapView = snapHelper.findSnapView(layoutManager);
                    if (snapView == null || musicList.isEmpty()) return;
                    currentAbsPos = layoutManager.getPosition(snapView);
                    int realIdx = currentAbsPos % musicList.size();
                    if (realIdx != currentIndex) {
                        stopMediaPlayer();
                        currentIndex = realIdx;
                        refreshUI(musicList.get(currentIndex));
                    }
                }
            }
        });

        // 初始化列表占位
        musicList = new ArrayList<>();
        Music tempHolder = new Music();
        tempHolder.setName("加载中...");
        tempHolder.setSinger("等待B站数据");
        musicList.add(tempHolder);

        wheelAdapter = new SongWheelAdapter(musicList, realPos -> {
            if (musicList.isEmpty()) return;
            if (realPos != currentIndex) {
                stopMediaPlayer();
            }
            currentIndex = realPos % musicList.size();
            refreshUI(musicList.get(currentIndex));
        });
        rvSongWheel.setAdapter(wheelAdapter);

        int totalItemCount = musicList.size() * 100;
        int midBase = (totalItemCount / 2 / musicList.size()) * musicList.size();
        currentAbsPos = midBase;

        rvSongWheel.post(() -> {
            rvSongWheel.scrollToPosition(currentAbsPos - 1);
            handler.postDelayed(() -> {
                rvSongWheel.smoothScrollToPosition(currentAbsPos);
            }, 50);
        });

        currentIndex = currentAbsPos % musicList.size();
        refreshUI(musicList.get(currentIndex));

        btnPlay.setOnClickListener(v -> {
            isPlaying = !isPlaying;
            if (isPlaying) {
                btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                playCurrentSelectedMusic();
            } else {
                btnPlay.setImageResource(android.R.drawable.ic_media_play);
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    handler.removeCallbacks(progressRunnable);
                }
            }
        });

        // 上下切歌 仅±1位 短平滑无飞转
        btnPrev.setOnClickListener(v -> {
            if (scrollLock || musicList.isEmpty() || switchingSong) return;
            scrollLock = true;
            switchingSong = true;

            currentAbsPos -= 1;
            rvSongWheel.smoothScrollToPosition(currentAbsPos);

            handler.postDelayed(() -> {
                scrollLock = false;
                switchingSong = false;
            }, 350);
        });

        btnNext.setOnClickListener(v -> {
            if (scrollLock || musicList.isEmpty() || switchingSong) return;
            scrollLock = true;
            switchingSong = true;

            currentAbsPos += 1;
            rvSongWheel.smoothScrollToPosition(currentAbsPos);

            handler.postDelayed(() -> {
                scrollLock = false;
                switchingSong = false;
            }, 350);
        });

        requestWeather("Shanghai");
        loadNextBv();
    }

    /**
     * 毫秒格式化 00:00
     */
    private String timeFormat(long ms) {
        if (ms <= 0) return "00:00";
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return String.format("%02d:%02d", min, sec);
    }

    // 串行加载全部BV
    private void loadNextBv() {
        if (loadIndex >= bvArr.length) {
            runOnUiThread(() -> {
                // 删除加载占位条目
                musicList.remove(0);
                wheelAdapter.notifyDataSetChanged();

                // 重新计算无限轮盘中间基准位置
                int totalItemCount = musicList.size() * 100;
                int midBase = (totalItemCount / 2 / musicList.size()) * musicList.size();
                currentAbsPos = midBase;

                rvSongWheel.post(() -> {
                    rvSongWheel.scrollToPosition(currentAbsPos);
                    handler.postDelayed(() -> {
                        switchingSong = true;
                        // 只单次向右滑动一整项，模拟手指滑到下一页，PagerSnapHelper自动吸附居中
                        rvSongWheel.smoothScrollBy(450, 0);
                        handler.postDelayed(() -> {
                            switchingSong = false;
                            View centerView = snapHelper.findSnapView(layoutManager);
                            if (centerView != null) {
                                int pos = layoutManager.getPosition(centerView);
                                layoutManager.scrollToPositionWithOffset(pos, 0);
                                currentAbsPos = pos;
                                currentIndex = pos % musicList.size();
                                refreshUI(musicList.get(currentIndex));
                            }
                        }, 300);
                    }, 100);
                });
            });
            return;
        }
        String bv = bvArr[loadIndex];
        loadSingleBv(bv, () -> {
            loadIndex++;
            loadNextBv();
        });
    }

    private void loadSingleBv(String bv, Runnable finishCallback) {
        String reqUrl = String.format("http://%s:%d/bili/getVideoInfo?bv=%s", SERVER_IP, SERVER_PORT, bv);
        Request request = new Request.Builder()
                .url(reqUrl)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                runOnUiThread(() -> tvCity.setText("BV:" + bv + " 获取失败"));
                finishCallback.run();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null || !response.isSuccessful()) {
                    runOnUiThread(() -> tvCity.setText("BV:" + bv + " 接口异常"));
                    finishCallback.run();
                    return;
                }
                String json = response.body().string();
                BiliVideoBean bean = gson.fromJson(json, BiliVideoBean.class);

                runOnUiThread(() -> {
                    Music newMusic = new Music();
                    newMusic.setName(bean.getTitle());
                    newMusic.setSinger(bean.getUpName());
                    //newMusic.setCoverUrl(bean.getCoverUrl());
                    newMusic.setUrl(bean.getAudioProxyUrl());

                    musicList.add(newMusic);
                    wheelAdapter.notifyDataSetChanged();
                });
                finishCallback.run();
            }
        });
    }

    private void stopMediaPlayer() {
        handler.removeCallbacks(progressRunnable);
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.reset();
        }
        isPlaying = false;
        runOnUiThread(() -> {
            btnPlay.setImageResource(android.R.drawable.ic_media_play);
            seekBar.setProgress(0);
            // 切歌重置时间文字
            tv_current_time.setText("00:00");
            tv_total_time.setText("00:00");
        });
    }

    private void playCurrentSelectedMusic() {
        if (musicList.isEmpty()) {
            runOnUiThread(() -> tvCity.setText("暂无音频"));
            return;
        }
        Music selectMusic = musicList.get(currentIndex);
        String audioUrl = selectMusic.getUrl();
        if (audioUrl == null || audioUrl.isEmpty()) {
            runOnUiThread(() -> tvCity.setText("音频地址为空"));
            return;
        }
        try {
            mediaPlayer.reset();

            mediaPlayer.setOnPreparedListener(mp -> {
                mp.start();
                isPlaying = true;
                runOnUiThread(() -> {
                    btnPlay.setImageResource(android.R.drawable.ic_media_pause);
                    seekBar.setMax(mp.getDuration());
                    // 初始化总时长
                    tv_total_time.setText(timeFormat(mp.getDuration()));
                });
                handler.post(progressRunnable);
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                runOnUiThread(() -> tvCity.setText("音频播放失败"));
                return true;
            });

            mediaPlayer.setDataSource(audioUrl);
            mediaPlayer.prepareAsync();

        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> tvCity.setText("IO异常"));
        }
    }

    private void requestWeather(String cityName) {
        String url = "https://api.seniverse.com/v3/weather/now.json?key=SgxXerYVyiZMF4pEq&location=" + cityName + "&language=zh-Hans&unit=c";
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                new Handler(Looper.getMainLooper()).post(() -> tvCity.setText("天气请求失败"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.body() == null) return;
                String jsonStr = response.body().string();
                response.close();
                try {
                    JSONObject root = new JSONObject(jsonStr);
                    JSONObject result = root.getJSONArray("results").getJSONObject(0);
                    JSONObject now = result.getJSONObject("now");

                    String city = result.getJSONObject("location").getString("name");
                    String temp = now.getString("temperature") + "℃";
                    String weatherDesc = now.getString("text");

                    new Handler(Looper.getMainLooper()).post(() -> {
                        tvCity.setText(city);
                        tvTemp.setText(temp);
                        tvWeatherText.setText(weatherDesc);
                    });
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void initMockMusicData() {
    }

    private void refreshUI(Music music) {
        tvSongName.setText(music.getName());
        tvSinger.setText(music.getSinger());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}