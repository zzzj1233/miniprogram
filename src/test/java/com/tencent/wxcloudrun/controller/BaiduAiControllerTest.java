package com.tencent.wxcloudrun.controller;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONObject;
import com.tencent.wxcloudrun.config.ApiResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.Base64;

/**
 * @author zzzj
 * @create 2022-12-15 11:57
 */
class BaiduAiControllerTest {

    private BaiduAiController baiduAiController;

    private String image;

    @BeforeEach
    void setUp() throws Exception {
        baiduAiController = new BaiduAiController();

        ClassPathResource resource = new ClassPathResource("img1.jpg");

        image = Base64.getEncoder().encodeToString(IoUtil.readBytes(resource.getInputStream()));
    }

    @Test
    public void text() throws Exception {
        System.setProperty("名称","name");
        System.setProperty("纳税人识别号","taxAccount");
        System.setProperty("地址、电话","location");

        ApiResponse response = baiduAiController.textRecognition(
                new JSONObject().putOnce("image", this.image)
        );

        System.out.println(response.getData());
    }

    @Test
    public void splitTest() throws Exception {
        String word = "abc:cd";
        Assertions.assertEquals(word.split("：").length, 1);
        Assertions.assertEquals(word.split(":").length, 2);
    }

}