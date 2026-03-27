package com.mustafa.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.mustafa")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.mustafa")
@EnableScheduling
// 🚀 İŞTE EKSİK OLAN İKİ BÜYÜK RADAR:
@EnableJpaRepositories(basePackages = "com.mustafa.repository") // Repoları burada ara!
@EntityScan(basePackages = "com.mustafa.entity")                // Tabloları (Entity) burada ara!
public class BankCorporateMicroApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankCorporateMicroApplication.class, args);
    }
}