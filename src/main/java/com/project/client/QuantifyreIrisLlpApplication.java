package com.project.client;

import org.springframework.boot.SpringApplication;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories; // Naya Import
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
//1. Component Scan: "com.project" rakhein taaki client, agency, common sab cover ho jayein

@ComponentScan(basePackages = {"com.project"}) 

//2. Entity Scan: Saare models scan karein
@EntityScan(basePackages = {"com.project"}) 

//3. JPA Repositories: Check karein ki path sahi hain
@EnableJpaRepositories(basePackages = { 
 "com.project.client.repository.jpa",		
 "com.project.agency.repository.jpa",
 "com.project.common.repository.jpa"
})

//4. MongoDB Repositories
@EnableMongoRepositories(basePackages = {
 "com.project.common.repository.mongodb" 
})
public class QuantifyreIrisLlpApplication {
 public static void main(String[] args) {
     SpringApplication.run(QuantifyreIrisLlpApplication.class, args);
 }
}