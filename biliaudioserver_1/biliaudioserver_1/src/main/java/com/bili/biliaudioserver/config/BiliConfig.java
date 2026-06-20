package com.bili.biliaudioserver.config;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import java.util.concurrent.TimeUnit;

public class BiliConfig {
    // 从浏览器B站 F12 - Application - Cookie 复制 SESSDATA
    public static final String SESS_DATA = "6b160839%2C1796602023%2C692da%2A61CjDgw0BW_xAUK9ByiIZAlz8yTk_EuK79GU-VPsu8--21IbLqLS7P71cq2xeYrU29ynoSVnFmaTFoRE4wTzl6ZVI1SldZTUtQVEwwVXpURFN3eDd0OV9fTlVkTXJfUDhVX2M4dmlzaHJSYzRMcW5WV1hMbDV5ang4ZktXUVpLbC1Ib1FtdUI1dlFRIIEC";

    public static final OkHttpClient HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    // 构造通用请求头（必须带Referer+UA+Cookie，否则B站403）
    public static Request.Builder getBaseHeader() {
        return new Request.Builder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Referer", "https://www.bilibili.com/")
                .header("Cookie", "SESSDATA=" + SESS_DATA);
    }
}