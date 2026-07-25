package com.xhzb;


import cn.hutool.core.map.MapUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Map;


@Slf4j
public class weatherCaseTest {


    @Test
    void functionTest1() {
        Map<String, Object> queryParams = MapUtil.<String, Object>builder()
                .put("areaCode", "420100")
                .build();

        String result = HttpUtil.createGet("https://tianqi3.market.alicloudapi.com/hour24")
                .form(queryParams)
                .header("Authorization", "APPCODE 2d9c74ca7de147eab35ac42f79fc3e90")
                .execute()
                .body();

        log.info("天气是-------{}", result);
    }

    @Test
    void functionExpressTest1() {
        Map<String, Object> queryParams = MapUtil.<String, Object>builder()
                .put("waybillNo", "428191465648193")
                .build();

        String result = HttpUtil.createPost("https://lhkdcx.market.alicloudapi.com/express/query")
                // 1. 设置请求头：Authorization 和 Content-Type
                .header("Authorization", "APPCODE 2d9c74ca7de147eab35ac42f79fc3e90")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                // 2. 用 form() 传递表单参数，不是 body()
                .form(queryParams)
                .execute()
                .body();

        log.info("快递-------{}", result);
    }


}
