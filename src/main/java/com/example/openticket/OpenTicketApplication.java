package com.example.openticket;

import com.example.openticket.domain.queue.QueueProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(QueueProperties.class)
public class OpenTicketApplication {

    public static void main(String[] args) {
        SpringApplication.run(OpenTicketApplication.class, args);
    }

}
