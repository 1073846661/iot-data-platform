package com.iot.platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.config.DeepSeekConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class NL2SQLService {
    @Autowired
    private DeepSeekConfig deepSeekConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String buildPrompt(String question){
        return "你是一个MySQL SQL生成器。根据用户问题生成查询语句。\n" +
                "\n" +
                "可用表结构：\n" +
                "1. alert_record（告警记录表）：device_id(设备ID), alert_type(告警类型: HIGH_TEMP=高温, HUMIDITY_ABNORMAL=湿度异常), alert_message(告警内容), alert_time(告警时间)\n" +
                "2. device_data（设备数据表）：device_id(设备ID), temperature(温度), humidity(湿度), received_at(接收时间)\n" +
                "\n" +
                "要求：只输出SQL语句本身，不要任何解释和markdown标记。\n" +
                "不要给列起别名（不要使用AS）\n" +
                "今天是" + LocalDate.now() + "。\n" +
                "\n" +
                "用户问题：";
    }

    public String generateSql(String question) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", deepSeekConfig.getModel());   // "model": "deepseek-chat"

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("content", buildPrompt(question));

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", question);

        body.put("messages", List.of(systemMsg, userMsg));   // List.of = 快速建列表

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);   // 告诉对方：我发的是 JSON
        headers.set("Authorization", "Bearer " + deepSeekConfig.getApiKey());   // 密码在这

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);  // 请求体+请求头打包
        String response = restTemplate.postForObject(deepSeekConfig.getApiUrl(), entity, String.class);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response);          // 整棵树的根
            //  往下找：choices 是数组 → 取第0个 → 里面的 message → 里面的 content
            String sql = root.path("choices").get(0).path("message").path("content").asText();
            sql = sql.replace("```sql", "").replace("```", "").trim();
            int idx = sql.toLowerCase().indexOf("select");
            if (idx > 0) {
                sql = sql.substring(idx);
            }
            return sql;
        }catch (JsonProcessingException e){
            throw new RuntimeException("解析DeepSeek响应失败", e);
        }
    }

    public List<Map<String, Object>> executeSql(String sql) {
        return jdbcTemplate.queryForList(sql);
    }

    private void checkSelectOnly(String sql){
        String cleaned = sql.trim().toLowerCase();
        if (!cleaned.startsWith("select")){
            throw new IllegalArgumentException("只允许SELECT查询");
        }

        String[] dangerous = {"insert", "update", "delete", "drop", "alter", "truncate"};
        for (String keyword : dangerous){
            if (cleaned.contains(keyword)){
                throw new IllegalArgumentException("SQL包含禁止关键词: " + keyword);
            }
        }
    }

    private void checkTableWhiteList(String sql){
        Set<String> allowed = Set.of("alert_record", "device_data");
        Pattern p = Pattern.compile("(?i)(?:from|join)\\s+([a-z_][a-z0-9_]*)");
        Matcher m = p.matcher(sql);
        while (m.find()){
            String table = m.group(1);
            if (!allowed.contains(table)){
                throw new IllegalArgumentException("不允许查询表: " + table);
            }
        }
    }

    private String appendLimit(String sql){
        sql = sql.trim();
        if (sql.endsWith(";")){
            sql = sql.substring(0, sql.length() - 1).trim();
        }
        if (!sql.toLowerCase().contains("limit")){
            return sql + " LIMIT 100";
        }
        return sql;
    }

    public String applyGuardrails(String sql){
        checkSelectOnly(sql);
        checkTableWhiteList(sql);
        return appendLimit(sql);
    }
}
