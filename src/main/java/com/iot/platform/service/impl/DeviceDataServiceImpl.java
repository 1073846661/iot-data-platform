package com.iot.platform.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iot.platform.entity.DeviceData;
import com.iot.platform.mapper.DeviceDataMapper;
import com.iot.platform.service.DeviceDataService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;

@Service
public class DeviceDataServiceImpl extends ServiceImpl<DeviceDataMapper, DeviceData> implements DeviceDataService {
    @Override
    public List<DeviceData> listLatest(int limit) {
        LambdaQueryWrapper<DeviceData> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(DeviceData::getReceivedAt).last("limit " + limit);
        return list(wrapper);
    }

    @Override
    public Page<DeviceData> pageDeviceData(int current, int size, String deviceId, String startTime, String endTime) {
        LambdaQueryWrapper<DeviceData> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(deviceId != null, DeviceData::getDeviceId, deviceId);    //条件重载：null就跳
        wrapper.between(startTime != null && endTime != null, DeviceData::getReceivedAt, startTime, endTime);
        wrapper.orderByDesc(DeviceData::getReceivedAt);     //最新在前
        return page(new Page<>(current, size), wrapper);    //分页拦截器
    }

    @Override
    @Cacheable(cacheNames = "deviceData", key = "#id") // Redis key = "deviceData::1"
    public DeviceData getById(Serializable id){
        return super.getById(id);
    }

    @Override
    @CacheEvict(cacheNames = "deviceData", key = "#data.id") // 更新后删旧缓存
    public boolean updateById(DeviceData data){
        return super.updateById(data);
    }
}
