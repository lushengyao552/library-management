package com.lushengyao.library.controller;


import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Resource;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;

@RestController 
public class PingController {
    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
        @Resource
    private JdbcTemplate jdbcTemplate;
    
    @GetMapping("/db/check")
    public Map<String, Object> dbCheck() {
        String sql = "SELECT NOW() AS db_time, VERSION() AS db_version";
        return jdbcTemplate.queryForMap(sql);
    }
}
