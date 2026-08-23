package com.iot.platform.service;

import com.iot.platform.entity.AlertRecord;
import com.iot.platform.entity.DeviceData;
import com.iot.platform.mapper.AlertRecordMapper;
import com.iot.platform.rule.AlertRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertService {
    @Autowired
    private List<AlertRule> rules;

    @Autowired
    private AlertRecordMapper alertRecordMapper;

    public void checkAndAlert(DeviceData data){
        for (AlertRule rule : rules){
            if (rule.check(data)){
                AlertRecord alertRecord = new AlertRecord();
                alertRecord.setDeviceId(data.getDeviceId());
                alertRecord.setAlertType(rule.getAlertType());
                alertRecord.setAlertMessage(rule.getMessage(data));
                alertRecordMapper.insert(alertRecord);
                System.out.println("[告警触发]" + alertRecord.getAlertMessage());
            }
        }
    }
}
