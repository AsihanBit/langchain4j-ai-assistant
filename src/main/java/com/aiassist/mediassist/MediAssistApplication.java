package com.aiassist.mediassist;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.aiassist.mediassist.mapper")
public class MediAssistApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediAssistApplication.class, args);
    }

}
