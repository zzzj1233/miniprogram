package com.tencent.wxcloudrun.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.lang.UUID;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Zzzj
 * @create 2022-12-13 21:54
 */
@Service
@Slf4j
public class DbService implements AutoCloseable {

    private JSONArray jsonArray;

    private ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(1);

    private ClassPathResource classPathResource;

    @PostConstruct
    public void readFile() {
        classPathResource = new ClassPathResource("db.json");

        File file = classPathResource.getFile();

        this.jsonArray = JSONUtil.parseArray(classPathResource.readUtf8Str());

        log.info("Data = {} ", jsonArray);

        scheduledThreadPool.scheduleWithFixedDelay(() -> {

            log.info("Before write memory data to file");

            try {
                FileUtil.writeUtf8String(this.jsonArray.toString(), file);
            } catch (IORuntimeException e) {
                log.error("Write error : ", e);
            }

            log.info("After write memory data to file");

        }, 5, 1, TimeUnit.SECONDS);
    }

    @Override
    public void close() throws Exception {
        scheduledThreadPool.shutdown();
    }


    public JSONArray list() {
        return this.jsonArray;
    }

    public synchronized void add(JSONObject jsonObject) {
        jsonObject.set("id", UUID.randomUUID());

        this.jsonArray.add(jsonObject);
    }

    public synchronized boolean deleteById(String id) {
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            if (object.getStr("id").equals(id)) {
                jsonArray.remove(i);
                return true;
            }
        }
        return false;
    }

    public synchronized boolean editById(String id, JSONObject jsonObject) {
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            if (object.getStr("id").equals(id)) {
                jsonObject.set("id", id);
                jsonArray.set(i, jsonObject);
                return true;
            }
        }
        return false;
    }

}
