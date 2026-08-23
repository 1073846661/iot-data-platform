package com.iot.platform.rule;

import com.iot.platform.entity.DeviceData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TemperatureRule implements AlertRule{

    @Override
    public boolean check(DeviceData data) {
        BigDecimal temp = data.getTemperature();
        return temp.compareTo(BigDecimal.valueOf(50)) > 0;
    }

    @Override
    public String getAlertType() {
        return "HIGH_TEMP";
    }

    @Override
    public String getMessage(DeviceData data) {
        return "设备" + data.getDeviceId() + "温度过高：" + data.getTemperature() + "℃";
    }
}
