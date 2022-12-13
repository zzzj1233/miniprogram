package com.tencent.wxcloudrun.controller;

import cn.hutool.json.JSONObject;
import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.service.DbService;
import org.springframework.web.bind.annotation.*;

/**
 * @author Zzzj
 * @create 2022-12-13 22:01
 */
@RestController
@RequestMapping("/business")
public class DbController {

    private final DbService dbService;

    public DbController(DbService dbService) {
        this.dbService = dbService;
    }

    @GetMapping
    public ApiResponse list() {
        return ApiResponse.ok(dbService.list());
    }

    @PostMapping
    public ApiResponse add(@RequestBody JSONObject jsonObject) {
        dbService.add(jsonObject);
        return ApiResponse.ok();
    }

    @PutMapping
    public ApiResponse edit(@RequestBody JSONObject jsonObject) {
        return ApiResponse.ok(dbService.editById(jsonObject.getStr("id"), jsonObject));
    }

    @DeleteMapping
    public ApiResponse delete(@RequestBody JSONObject jsonObject) {
        return ApiResponse.ok(dbService.deleteById(jsonObject.getStr("id")));
    }

}
