package com.iot.platform.rule;

import com.iot.platform.entity.DeviceData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class HumidityRule implements AlertRule{
    @Override
    public boolean check(DeviceData data) {
        BigDecimal humidity = data.getHumidity();
        return humidity.compareTo(BigDecimal.valueOf(20)) < 0 ||
               humidity.compareTo(BigDecimal.valueOf(80)) > 0;
    }

    @Override
    public String getAlertType() {
        return "HUMIDITY_ABNORMAL";
    }

    @Override
    public String getMessage(DeviceData data) {
        return "设备" + data.getDeviceId() + "湿度异常：" + data.getHumidity() + "%";
    }
}
