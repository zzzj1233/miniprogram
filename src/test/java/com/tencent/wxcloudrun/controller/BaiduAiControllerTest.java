package com.tencent.wxcloudrun.controller;

import cn.hutool.core.map.MapUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

/**
 * @author zzzj
 * @create 2022-12-16 13:13
 */
class BaiduAiControllerTest {

    BaiduAiController controller = new BaiduAiController(null);

    @Test
    public void splitTest() throws Exception {
        String word = "abc:";

        Assertions.assertTrue(word.split(":", 2).length == 2);
        Assertions.assertEquals("", word.split(":", 2)[1]);
    }

    @Test
    public void doMatchTest() throws Exception {
        Map<String, String> matchResult = controller.doMatch(
                Arrays.asList(
                        "址：",
                        "ABC",
                        "EFG",
                        "名称：123",
                        "电",
                        "话:",
                        "1233"
                ),
                MapUtil.<String, String>builder()
                        .put("地址", "location")
                        .put("名称", "name")
                        .put("电话", "phone")
                        .build()
        );

        Assertions.assertEquals(3, matchResult.size());
        Assertions.assertEquals("ABC", matchResult.get("location"));
        Assertions.assertEquals("123", matchResult.get("name"));
        Assertions.assertEquals("1233", matchResult.get("phone"));
    }

}