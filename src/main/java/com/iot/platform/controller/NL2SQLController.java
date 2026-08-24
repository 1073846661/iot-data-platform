package com.iot.platform.controller;

import com.iot.platform.service.NL2SQLService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class NL2SQLController {
    @Autowired
    private NL2SQLService nl2SQLService;

    @PostMapping("/api/query")
    public Map<String, Object> query(@RequestBody Map<String, String> req){
         String question = req.get("question");
        String sql = nl2SQLService.generateSql(question);
        List<Map<String, Object>> data = nl2SQLService.executeSql(sql);
        Map<String, Object> result = new HashMap<>();
        result.put("question", question);
        result.put("sql", sql);
        result.put("data", data);
        return result;
    }
}
