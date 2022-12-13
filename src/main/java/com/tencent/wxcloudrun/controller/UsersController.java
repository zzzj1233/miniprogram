package com.tencent.wxcloudrun.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.tencent.wxcloudrun.config.ApiResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Zzzj
 * @create 2022-12-13 22:37
 */
@RestController
@RequestMapping("/users")
public class UsersController {

    private final JdbcTemplate jdbcTemplate;

    public UsersController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping
    public ApiResponse list() {
        List<Map<String, Object>> list = jdbcTemplate.queryForList("SELECT * FROM users");

        return ApiResponse.ok(
                list.stream()
                        .filter(it -> it.get("data") != null && StrUtil.isNotBlank(it.get("data").toString()))
                        .map(it -> new JSONObject(it.get("data").toString()).putOnce("id", it.get("id")))
                        .collect(Collectors.toList())
        );
    }

    @PostMapping
    public ApiResponse add(@RequestBody Map<String, Object> map) {
        String sql = "INSERT INTO users (`data`) values (  " + StrUtil.wrap(JSONUtil.toJsonStr(map), "'") + ")";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> connection.prepareStatement(sql, new String[]{"id"}), keyHolder);

        return ApiResponse.ok(keyHolder.getKey().intValue());
    }

    @PutMapping
    public ApiResponse update(@RequestBody JSONObject jsonObject) {
        String id = jsonObject.getStr("id");
        String data = jsonObject.getStr("data");

        if (StrUtil.isBlank(id)) {
            return ApiResponse.error("missing id");
        }

        String sql = "UPDATE users set `data` = ? WHERE id = ?";

        jdbcTemplate.update(sql, data, id);

        return ApiResponse.ok();
    }

    @DeleteMapping
    public ApiResponse delete(@RequestBody JSONObject jsonObject) {
        String id = jsonObject.getStr("id");

        if (StrUtil.isBlank(id)) {
            return ApiResponse.error("missing id");
        }

        jdbcTemplate.update("DELETE FROM users where id = ?", id);

        return ApiResponse.ok();
    }


}
