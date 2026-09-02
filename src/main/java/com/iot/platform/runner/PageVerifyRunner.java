package com.iot.platform.runner;


import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.iot.platform.entity.DeviceData;
import com.iot.platform.service.DeviceDataService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class PageVerifyRunner implements CommandLineRunner {

    private final DeviceDataService service;

    public PageVerifyRunner(DeviceDataService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) throws Exception {
        Page<DeviceData> page = service.page(new Page<>(1, 5), null);
        System.out.println("total = " + page.getTotal());
        System.out.println("本页条数 = " + page.getRecords().size());
    }
}
