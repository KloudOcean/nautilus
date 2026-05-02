package com.kloudocean.nautilus;

import com.kloudocean.nautilus.config.NautilusProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(NautilusProperties.class)
public class NautilusApplication {

    public static void main(String[] args) {
        SpringApplication.run(NautilusApplication.class, args);
    }
}
