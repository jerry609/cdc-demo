package com.example.cdcdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableCaching
@EnableAsync
@MapperScan("com.example.cdcdemo.mapper")
public class CdcDemoApplication {
    public static void main(String[] args) {
        SpringApplication.run(CdcDemoApplication.class, args);
    }
}