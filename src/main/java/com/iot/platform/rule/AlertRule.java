package com.iot.platform.rule;

import com.iot.platform.entity.DeviceData;

public interface AlertRule {
    boolean check(DeviceData data);
    String getAlertType();
    String getMessage(DeviceData data);
}
