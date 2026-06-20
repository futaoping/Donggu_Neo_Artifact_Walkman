package com.bili.biliaudioserver.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bili.biliaudioserver.config.BiliConfig;
import com.bili.biliaudioserver.entity.BiliVideoInfo;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;

@RestController
public class BiliController {

    /**
     * 根据BV号获取视频标题、UP、封面、cid
     */
    @GetMapping("/bili/getVideoInfo")
    public ResponseEntity<BiliVideoInfo> getVideoInfo(@RequestParam String bv) throws IOException {
        String url = "https://api.bilibili.com/x/web-interface/view?bvid=" + bv;
        Request request = BiliConfig.getBaseHeader()
                .url(url)
                .get()
                .build();

        try (Response resp = BiliConfig.HTTP_CLIENT.newCall(request).execute()) {
            String body = resp.body().string();
            JSONObject json = JSON.parseObject(body);
            JSONObject data = json.getJSONObject("data");

            BiliVideoInfo info = new BiliVideoInfo();
            info.setTitle(data.getString("title"));
            info.setCoverUrl(data.getString("pic"));
            info.setUpName(data.getJSONObject("owner").getString("name"));
            Long cid = data.getJSONArray("pages").getJSONObject(0).getLong("cid");
            info.setCid(cid);

            // ========== 修改处：替换为电脑局域网IP+真实后端端口8081 ==========
            String localAudioUrl = "http://192.168.0.101:8081/bili/proxyAudio?bv=" + bv + "&cid=" + cid;
            info.setAudioProxyUrl(localAudioUrl);

            return ResponseEntity.ok(info);
        }
    }

    /**
     * 音频流代理转发，绕过B站403防盗链
     */
    @GetMapping("/bili/proxyAudio")
    public void proxyAudio(@RequestParam String bv,
                           @RequestParam Long cid,
                           HttpServletResponse response) throws IOException {

        String playUrl = "https://api.bilibili.com/x/player/playurl?bvid=" + bv + "&cid=" + cid + "&fnval=4048";
        Request reqPlay = BiliConfig.getBaseHeader()
                .url(playUrl)
                .get()
                .build();

        try (Response respPlay = BiliConfig.HTTP_CLIENT.newCall(reqPlay).execute()) {
            String playBody = respPlay.body().string();
            JSONObject playJson = JSON.parseObject(playBody);
            JSONObject dash = playJson.getJSONObject("data").getJSONObject("dash");
            String audioRealUrl = dash.getJSONArray("audio").getJSONObject(0).getString("baseUrl");

            Request audioReq = BiliConfig.getBaseHeader()
                    .url(audioRealUrl)
                    .get()
                    .build();

            try (Response audioResp = BiliConfig.HTTP_CLIENT.newCall(audioReq).execute();
                 InputStream is = audioResp.body().byteStream();
                 ServletOutputStream os = response.getOutputStream()) {

                response.setContentType("audio/mpeg");
                byte[] buffer = new byte[4096];
                int len;
                while ((len = is.read(buffer)) != -1) {
                    os.write(buffer, 0, len);
                }
                os.flush();
            }
        }
    }
}