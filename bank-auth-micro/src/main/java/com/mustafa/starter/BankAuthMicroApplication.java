package com.mustafa.starter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@SpringBootApplication
@ComponentScan(basePackages = {"com.mustafa"})
@EntityScan(basePackages = {"com.mustafa.entity"})
@EnableJpaRepositories(basePackages = {"com.mustafa.repository"})
@EnableFeignClients(basePackages = {"com.mustafa.client"})
public class BankAuthMicroApplication {

    public static void main(String[] args) {
        SpringApplication.run(BankAuthMicroApplication.class, args);
    }

}
