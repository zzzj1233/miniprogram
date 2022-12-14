package com.tencent.wxcloudrun.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.tencent.wxcloudrun.config.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Zzzj
 * @create 2022-12-13 22:37
 */
@RestController
@RequestMapping("/users")
@Slf4j
public class UsersController {

    private final JdbcTemplate jdbcTemplate;

    public UsersController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse list() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM users");

        return ApiResponse.ok(list.stream()
                .map(it -> {
                    JSONObject jsonObject = new JSONObject();

                    jsonObject.putOnce("name", it.get("name"));

                    JSONObject data = Optional.ofNullable(it.get("data"))
                            .map(JSONObject::new)
                            .orElse(new JSONObject());

                    jsonObject.putAll(data);

                    return jsonObject;
                }).collect(Collectors.toList()));
    }

    @PostMapping
    public ApiResponse add(@RequestBody JSONObject jsonObject) {
        // 1. 检查用户名是否存在
        String name = jsonObject.getStr("name");

        String data = jsonObject.getStr("data");

        if (StrUtil.isBlank(name)) {
            return ApiResponse.error("名称不能为空");
        }

        Integer count = jdbcTemplate.queryForObject("SELECT count(1) as c FROM users WHERE name = ?", (rs, rowNum) -> rs.getInt("c"), name);

        if (count > 0) {
            return ApiResponse.error("用户名已存在");
        }

        try {
            jdbcTemplate.update("INSERT INTO users (name, data) values (?, ?)", name, data);
        } catch (DataAccessException e) {
            log.error("Insert user error : ", e);
            return ApiResponse.error("用户名已存在");
        }

        return ApiResponse.ok();
    }

    @PostMapping("/update")
    public ApiResponse update(@RequestBody JSONObject jsonObject) {
        String name = jsonObject.getStr("name");

        String data = jsonObject.getStr("data");

        if (StrUtil.isBlank(name)) {
            return ApiResponse.error("名称不能为空");
        }

        Integer count = jdbcTemplate.queryForObject("SELECT count(1) as c FROM users WHERE name = ?", (rs, rowNum) -> rs.getInt("c"), name);

        if (count == 0) {
            return ApiResponse.error("用户不存在");
        }

        String sql = "UPDATE users set `data` = ? WHERE name = ?";

        jdbcTemplate.update(sql, data, name);

        return ApiResponse.ok();
    }

    @PostMapping("/delete")
    public ApiResponse delete(@RequestBody JSONObject jsonObject) {
        String name = jsonObject.getStr("name");

        if (StrUtil.isBlank(name)) {
            return ApiResponse.error("名称不能为空");
        }
        jdbcTemplate.update("DELETE FROM users where name = ?", name);

        return ApiResponse.ok();
    }


}
