package com.mustafa.starter; // Senin ana sınıfının olduğu paket

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.mustafa") // Component, Controller, Security ve Service'leri bulması için
@EntityScan(basePackages = "com.mustafa.entity") // Tablolarımızı (Customer, Account vb.) bulması için
@EnableJpaRepositories(basePackages = "com.mustafa.repository") // Birazdan yazacağımız DB arayüzlerini bulması için
@EnableScheduling // 🚀 ZAMANLANMIŞ GÖREV MOTÖRÜNÜ AKTİF EDER
public class BankDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankDemoApplication.class, args);
    }
}