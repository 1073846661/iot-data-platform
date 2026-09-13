package com.iot.platform.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.platform.annotation.Log;
import com.iot.platform.entity.DeviceData;
import com.iot.platform.mqtt.MqttClientManager;
import com.iot.platform.service.DeviceDataCacheService;
import com.iot.platform.service.DeviceDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/device")
public class DeviceDataController {
    @Autowired
    private DeviceDataService deviceDataService;
    @Autowired
    private DeviceDataCacheService deviceDataCacheService;
    @Autowired
    private MqttClientManager mqttClientManager;


    //POST /device - 插入一条设备数据
    @PostMapping
    public boolean add(@RequestBody DeviceData data){
        return deviceDataService.save(data); //IService.save()
    }

    //GET /device/1 - 根据ID查询
    @GetMapping("/{id}")
    public DeviceData getById(@PathVariable Long id){
        return deviceDataService.getById(id); //IService.getById()
    }

    //GET /device/latest?limit=10 - 查最新N条
    @GetMapping("/latest")
    @Log
    public List<DeviceData> latest(@RequestParam(defaultValue = "10") int limit){
        return deviceDataService.listLatest(limit);
    }

    @PutMapping
    public boolean update(@RequestBody DeviceData data){
        return deviceDataService.updateById(data);
    }

    @GetMapping("/safe/{id}")
    public DeviceData getByIdSafe(@PathVariable Long id){
        return deviceDataCacheService.getByIdSafe(id);
    }

    @GetMapping("/lock/{id}")
    public DeviceData getByIdWithLock(@PathVariable Long id){
        return deviceDataCacheService.getByIdWithLock(id);
    }

    @PostMapping("/{deviceId}/cmd")
    public boolean sendCmd(@PathVariable String deviceId, @RequestBody String cmd){
        String topic = "device/" + deviceId + "/cmd";   //拼出目标主题
        return mqttClientManager.publish(topic, cmd);   //交给MQTT客户端发布
    }

    @GetMapping("/page")
    @Log
    public Page<DeviceData> page(@RequestParam(defaultValue = "1") int current,
                                 @RequestParam(defaultValue = "10") int size,
                                 @RequestParam(required = false) String deviceId){
        return deviceDataService.pageDeviceData(current, size, deviceId);
    }
}
