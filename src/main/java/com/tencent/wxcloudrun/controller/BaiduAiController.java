package com.tencent.wxcloudrun.controller;

import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import com.tencent.wxcloudrun.config.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author zzzj
 * @create 2022-12-15 11:41
 */
@Slf4j
@RestController
@RequestMapping("/baidu")
public class BaiduAiController {

    private static final String CLIENT_ID = "HwdWWqgcNYGOYQl0NgbvEniR";

    private static final String CLIENT_SECRET = "EGzYaNX9tuWkBf5eDgWGye4ioq5e7VTf";

    private static final String URL = "https://aip.baidubce.com/oauth/2.0/token";

    private static long expireTime;

    private static String accessToken;


    private static void requestAccessToken() {
        String url = URL + StrUtil.format("?client_id={}&client_secret={}&grant_type=client_credentials", CLIENT_ID, CLIENT_SECRET);

        try {
            HttpResponse httpResponse = HttpUtil.createPost(url)
                    .header("Accept", "application/json")
                    .setReadTimeout(2000)
                    .setReadTimeout(2000)
                    .execute();
            log.info("Request access token response = {} ", httpResponse);
            if (httpResponse.isOk()) {

                JSONObject jsonObject = new JSONObject(httpResponse.body());

                BaiduAiController.accessToken = jsonObject.getStr("access_token");

                Integer expires = jsonObject.getInt("expires_in");

                expireTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(expires);
            }
        } catch (Exception e) {
            log.error("Request access token error : ", e);
        }
    }

    @PostMapping("/text")
    public ApiResponse textRecognition(
            @RequestBody JSONObject params
    ) {
        String image = params.getStr("image");

        if (StrUtil.isBlank(image)) {
            return ApiResponse.error("未上传图片");
        }

        if (System.currentTimeMillis() >= expireTime) {
            requestAccessToken();
        }

        // 本地文件路径
        String imgParam = URLEncodeUtil.encode(image, StandardCharsets.UTF_8);

        HttpRequest request = HttpUtil
                .createPost("https://aip.baidubce.com/rest/2.0/ocr/v1/general_basic?access_token=" + accessToken)
                .form("image", image);

        log.info("textRecognition request = {} ", request);

        HttpResponse httpResponse = request.execute();

        log.info("textRecognition response = {} ", httpResponse);

        if (!httpResponse.isOk()) {
            return ApiResponse.error("文字识别失败");
        }

        JSONObject jsonObject = new JSONObject(httpResponse.body());

        JSONArray wordsResult = jsonObject.getJSONArray("words_result");

        if (wordsResult.isEmpty()) {
            return ApiResponse.error("未失败到文字");
        }

        boolean strict = Boolean.parseBoolean(System.getProperty("strict", "false"));

        Map<String, String> matchResult = new HashMap<>();

        for (Object o : wordsResult) {
            JSONObject word = new JSONObject(o);

            String words = word.getStr("words");

            doMatch(strict, words, matchResult);
        }

        return ApiResponse.ok(matchResult);
    }

    public static void doMatch(boolean strict,
                               String words,
                               Map<String, String> matchResult) {

        String[] kv = words.split("：");

        if (kv.length <= 1) {
            kv = words.split(":");
        }

        if (kv.length <= 1) {
            return;
        }

        String name = kv[0];

        String value = kv[1];

        Properties properties = System.getProperties();

        Map<String, String> props = new HashMap<>(System.getenv());

        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String k = entry.getKey().toString();

            String v = entry.getValue().toString();

            if (StrUtil.isBlank(k) || StrUtil.isBlank(v) || props.containsKey(k)) {
                continue;
            }

            props.put(k, v);
        }


        if (strict) {
            if (props.containsKey(name)) {
                matchResult.put(name, value);
            }
        } else {

            for (Map.Entry<String, String> entry : props.entrySet()) {

                String k = entry.getKey();

                String v = entry.getValue();

                if (StrUtil.isBlank(k) || StrUtil.isBlank(v)) {
                    continue;
                }

                if (k.contains(name)) {
                    matchResult.put(v, value);
                    return;
                }

            }

        }
    }


}
