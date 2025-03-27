package com.tu.tucloudpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan(basePackages = "com.tu.tucloudpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class TuCloudpictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TuCloudpictureBackendApplication.class, args);
    }

}
