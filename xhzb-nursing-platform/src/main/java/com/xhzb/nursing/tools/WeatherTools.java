package com.xhzb.nursing.tools;

import cn.hutool.http.HttpUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component // 注册为一个组件
public class WeatherTools {

    @Tool(name = "queryWeather", description = "根据城市id查询天气信息")
    public String getWeather(@ToolParam(description = "城市id") String cityId) {

        // 天气请求URL
        String url = "http://t.weather.itboy.net/api/weather/city/" + cityId;
        return HttpUtil.get(url);
        //// 模拟返回天气信息
        //return WeatherDto.builder()
        //        .cityId(cityId) // 城市ID
        //        .city("北京") // 城市名称
        //        .temperature("25")   // 当前温度
        //        .lowTemperature("20")// 低温
        //        .highTemperature("30")// 高温
        //        .date("2025-07-05")// 数据日期
        //        .quality("优")// 空气质量
        //        .pm25(15.5)// PM2.5数值
        //        .build();
    }

}