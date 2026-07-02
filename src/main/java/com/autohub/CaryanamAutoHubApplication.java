package com.autohub;

import com.autohub.dto.WhatsAppProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(WhatsAppProperties.class)
@EnableAsync
@EnableRetry
@Slf4j
public class CaryanamAutoHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(CaryanamAutoHubApplication.class, args);
        System.out.println("CaryanamAutoHub Application started");
        System.out.println("\n\n");
        System.err.println("PORT : localhost8081");
        System.err.println("  *****    *******  *******       *****   *******    *****    ******   *******" );
        System.err.println(" *     *   *      *    *         *           *      *     *   *     *     *   " );
        System.err.println("*       *  *      *    *         *           *     *       *  *     *     *   " );
        System.err.println("*       *  *******     *          *****      *     *       *  ******      *   " );
        System.err.println("*********  *           *               *     *     *********  *   *       *   " );
        System.err.println("*       *  *           *               *     *     *       *  *    *      *   " );
        System.err.println("*       *  *        *******       *****      *     *       *  *     *     *   " );
    }





}
