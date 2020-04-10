package com.tiddar.raf;

import com.tiddar.raf.domain.Node;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


@SpringBootApplication
@EnableConfigurationProperties({Node.class})
public class RafApplication {

    public static void main(String[] args) {
        SpringApplication.run(RafApplication.class, args);
    }


}
