package com.tencent.wxcloudrun.controller;

import cn.hutool.core.net.URLEncodeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import com.tencent.wxcloudrun.config.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    private final JdbcTemplate jdbcTemplate;

    public BaiduAiController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

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

    @PostMapping("match")
    public ApiResponse match(
            @RequestBody JSONObject params
    ) {
        List<String> words = params.getJSONArray("words").toList(String.class);

        List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM `mapping`");

        Map<String, String> mapping = list.stream()
                .collect(Collectors.toMap(map -> map.get("key").toString(), map -> map.get("value").toString()));

        return ApiResponse.ok(doMatch(words, mapping));
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

        List<String> wordsList = jsonObject.getJSONArray("words_result").toList(JSONObject.class)
                .stream().map(jo -> jo.getStr("words"))
                .collect(Collectors.toList());

        if (wordsList.isEmpty()) {
            return ApiResponse.error("未失败到文字");
        }

        List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM `mapping`");

        Map<String, String> mapping = list.stream()
                .collect(Collectors.toMap(map -> map.get("key").toString(), map -> map.get("value").toString()));

        Map<String, String> matchResult = doMatch(wordsList, mapping);

        return ApiResponse.ok(matchResult);
    }

    Map<String, String> doMatch(List<String> wordsList,
                                        Map<String, String> mapping) {


        Map<String, String> matchResult = new HashMap<>();

        for (int i = 0; i < wordsList.size(); i++) {
            String word = wordsList.get(i);

            String sep;

            if (word.contains(":")) {
                sep = ":";
            } else if (word.contains("：")) {
                sep = "：";
            } else {
                continue;
            }

            String[] words = word.split(sep, 2);

            // value在下一行
            String name = words[0];

            String value = words[1];

            for (Map.Entry<String, String> entry : mapping.entrySet()) {

                String k = entry.getKey();

                String v = entry.getValue();

                if (k.contains(name)) {
                    if (StrUtil.isBlank(value)) {
                        // value在下一行
                        if (i + 1 < wordsList.size()) {
                            String nextLine = wordsList.get(i + 1);
                            matchResult.put(v, nextLine);
                            i++;
                        }
                    } else {
                        matchResult.put(v, value);
                    }
                    break;
                }

            }
        }
        return matchResult;
    }


}
