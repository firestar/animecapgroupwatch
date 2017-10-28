package com.animecap.groupwatch;

import com.animecap.groupwatch.config.WebSocketConfiguration;
import com.animecap.groupwatch.repositories.AnimecapAPIService;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.io.File;

/**
 * Created by Nathaniel on 10/28/2017.
 */
@SpringBootApplication
@EnableScheduling
@EnableDiscoveryClient
@EnableAutoConfiguration
@Import({WebSocketConfiguration.class})
public class GroupWatch {
    public static final String API_SERVICE_URL = "API";

    @LoadBalanced
    @Bean
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public AnimecapAPIService animecapAPIService() {
        return new AnimecapAPIService(API_SERVICE_URL);
    }

    public static void main(String[] args){
        new SpringApplicationBuilder().sources(GroupWatch.class).run(args);
    }
}
