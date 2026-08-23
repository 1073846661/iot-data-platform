package com.iot.platform.mqtt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.platform.config.MqttConfig;
import com.iot.platform.entity.DeviceData;
import com.iot.platform.service.AlertService;
import com.iot.platform.service.DeviceDataService;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Component
public class MqttClientManager implements MqttCallback, InitializingBean {
    @Autowired
    private MqttConfig mqttConfig;
    private MqttClient mqttClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DeviceDataService deviceDataService;

    @Autowired
    private AlertService alertService;


    public boolean publish(String topic, String payload){
        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            mqttClient.publish(topic, message);
            System.out.println("[MQTT 已下发] topic=" + topic + ", payload=" + payload);
            return true;
        } catch (MqttException e){
            System.out.println("[MQTT 下发失败] " + e.getMessage());
            return false;
        }
    }

    @Override
    public void connectionLost(Throwable cause){
        System.out.println("[MQTT 连接断开!] 原因: " + cause);
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                    try {
                        mqttClient.connect();
                        mqttClient.subscribe(mqttConfig.getTopic(), mqttConfig.getQos());
                        System.out.println("[MQTT 重连成功]");
                        break;
                    } catch (MqttException e){
                        System.out.println("[MQTT 重连失败，5秒后再试]");
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie){
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            }
        }).start();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
        try {
            DeviceData data = objectMapper.readValue(payload, DeviceData.class);
            data.setReceivedAt(LocalDateTime.now());
            System.out.println("[MQTT 数据入库] 设备=" + data.getDeviceId()
                    + ", 温度=" + data.getTemperature()
                    + ", 湿度=" + data.getHumidity()
                    + ", 接收时间=" + data.getReceivedAt());
            deviceDataService.save(data);
            alertService.checkAndAlert(data);
        } catch (JsonProcessingException e) {
            System.out.println("[MQTT 坏消息] 已丢弃，原因: " + e.getMessage());
        }
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token){
        System.out.println("[MQTT 送达确认]");
    }

    @Override
    public void afterPropertiesSet() throws Exception{
        mqttClient = new MqttClient(mqttConfig.getBrokerUrl(), mqttConfig.getClientId());
        mqttClient.setCallback(this);
        mqttClient.connect();
        mqttClient.subscribe(mqttConfig.getTopic(), mqttConfig.getQos());
        System.out.println("[MQTT] 已订阅: " + mqttConfig.getTopic());
    }

}
