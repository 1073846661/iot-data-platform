package com.iot.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@MapperScan("com.iot.platform.mapper")
@EnableCaching
public class IotDataPlatformApplication {

	public static void main(String[] args) {
		SpringApplication.run(IotDataPlatformApplication.class, args);
	}

}
