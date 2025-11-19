package org.hzj.demo;
import org.hzj.demo.config.BlogProperties; // 1. 导入
import org.hzj.demo.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableConfigurationProperties({
        BlogProperties.class,
        JwtProperties.class   // 2. 激活
}) // 3. 激活！
@EnableCaching // 2. 激活“自动挡”缓存功能
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);

    }

}
