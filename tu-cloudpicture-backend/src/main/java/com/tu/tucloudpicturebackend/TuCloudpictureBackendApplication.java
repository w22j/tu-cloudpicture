package com.tu.tucloudpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan(basePackages = "com.tu.tucloudpicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class TuCloudpictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(TuCloudpictureBackendApplication.class, args);
    }

}
