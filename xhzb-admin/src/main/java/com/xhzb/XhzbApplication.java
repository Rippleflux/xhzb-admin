package com.xhzb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 启动程序
 *
 * @author ruoyi
 */
@EnableAsync
@SpringBootApplication
public class XhzbApplication
{
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(com.xhzb.XhzbApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  星海智伴启动成功   ლ(´ڡ`ლ)ﾞ ");
    }
}
