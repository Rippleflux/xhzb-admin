package com.xhzb;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import com.xhzb.nursing.service.WechatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@Slf4j
@SpringBootTest
public class WechatTest {

    @Autowired
    WechatService wechatService;

    @Test
    public void getOpenIdTest() {
        String openid = wechatService.getOpenid("0c32MqHa1BPl6M0iRWGa1ykzCB02MqHS");
        log.info("openid是{}", openid);
    }

    @Test
    public void getPhoneTest() {
        String phoneStr = wechatService.getPhone("db0c59b1623ea07d1eff27da1b2c9f0f5bbd7554ccd49eb2bf68bfae501f4f3e");
        log.info("phoneStr----------------{}", phoneStr);
    }
}
