package com.iot.platform.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.iot.platform.entity.DeviceData;

import java.util.List;

public interface DeviceDataService extends IService<DeviceData> {
    //查最新N条数据（按时间倒序）
    List<DeviceData> listLatest(int limit);
    Page<DeviceData> pageDeviceData(int current, int size, String deviceId);
}
