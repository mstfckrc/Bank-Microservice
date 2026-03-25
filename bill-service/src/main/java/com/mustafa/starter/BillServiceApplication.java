package com.mustafa.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

// 🚀 RADAR GENİŞLETİLDİ: Artık com.mustafa altındaki HER ŞEYİ görecek!
@SpringBootApplication(scanBasePackages = "com.mustafa")
@EntityScan(basePackages = "com.mustafa.entity")
@EnableJpaRepositories(basePackages = "com.mustafa.repository")
@EnableFeignClients(basePackages = "com.mustafa.client")
@EnableDiscoveryClient
@EnableScheduling // 🚀 YENİ: Otomasyon motorunu uyandır!
public class BillServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillServiceApplication.class, args);
    }
}