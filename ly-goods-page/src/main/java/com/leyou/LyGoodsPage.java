package com.leyou;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * User: 清角吹寒
 * Date: 2019-02-14
 * Time: 19:28
 */
@EnableDiscoveryClient
@EnableFeignClients
@SpringBootApplication
public class LyGoodsPage {
    public static void main(String[] args) {
        SpringApplication.run(LyGoodsPage.class, args);
    }
}
