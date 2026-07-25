package com.xhzb;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HttpTest {
    /**
     * 注意：获取验证码本项目中，SpringSecurity做了放行
     * 使用hutool中的HttpUtil工具类发起请求
     * URL：GET http://localhost:9000/captchaImage
     * 参数：无
     */
    @Test
    @DisplayName("1-GET无参请求")
    void getTest1() {
        String result = HttpUtil.get("http://localhost:8080/captchaImage");
        log.info(result);
    }


    /**
     * 使用hutool中的HttpUtil工具类发起请求
     * URL： GET http://localhost:9000/nursing/project/list?pageNum=1&pageSize=5
     * 携带header请求头：Authorization=1asd#3sd4567890
     */
    @Test
    @DisplayName("2-GET带表单参数且可配置请求")
    void getTest2() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("pageNum", 1);
        data.put("pageSize", 5);

        String result = HttpUtil.createGet("http://localhost:8080/nursing/project/list")
                .form(data)
                .header("Authorization", "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImxvZ2luX3VzZXJfa2V5IjoiMzQ2MjdjNzMtYzBkZS00ZmJiLWI1YjgtYTk5N2E4ZTAyY2U2In0.Hfq-QIJjSFCYNkABBXfbOuRjjlIhbFxHNWb7-upbHGcs7sIsyaNqnA4FbDKF9jiy_6rvm4-J1vD7pyHIA3W_7w")
                .execute()
                .body();
        log.info(result);
    }


    /**
     * 注释掉：@PreAuthorize("@ss.hasPermi('nursing:project:list')")和在SecurityConfig中加"/nursing/project/**"放行
     * 使用hutool中的HttpUtil工具类发起请求
     * URL： GET http://localhost:9000/nursing/project/list?pageNum=1&pageSize=5
     * 参数：url中带有pageNum和pageSize参数
     */
    @Test
    @DisplayName("3-GET带params参数请求")
    void getTest3() {
        HashMap<String, Object> data = new HashMap<>();
        data.put("pageNum", 1);
        data.put("pageSize", 5);
        String result = HttpUtil.get("http://localhost:8080/nursing/project/list", data);
        log.info(result);
    }

    /**
     * 使用hutool中的HttpUtil工具类发起请求
     * URL： POST http://localhost:9000/nursing/project
     * 参数：
     * {
     * "image": "https://img2.baidu.com/it/u=3227619927,365499885&fm=253&fmt=auto&app=120&f=JPEG?w=938&h=500",
     * "name": "测试护理项目1",
     * "nursingRequirement": "测试护理项目1",
     * "orderNo": 1,
     * "price": 100,
     * "remark": "测试护理项目1",
     * "status": 1,
     * "unit": "次"
     * }
     */
    @Test
    @DisplayName("1-POST可配置请求")
    void postTest1() {

        // 构建bodyMap
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("image", "https://img2.baidu.com/it/u=3227619927,365499885&fm=253&fmt=auto&app=120&f=JPEG?w=938&h=500");
        bodyMap.put("name", "测试护理项目AI2");
        bodyMap.put("nursingRequirement", "测试护理项目AI2");
        bodyMap.put("orderNo", 1);
        bodyMap.put("price", 100);
        bodyMap.put("remark", "测试护理项目AI2");
        bodyMap.put("status", 1);
        bodyMap.put("unit", "次");
        // 转换成json字符串
        String bodyStr = JSONUtil.toJsonStr(bodyMap);

        String result = HttpUtil.createPost("http://localhost:8080/nursing/project")
                .body(bodyStr)
                .header("Authorization", "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsImxvZ2luX3VzZXJfa2V5IjoiMzQ2MjdjNzMtYzBkZS00ZmJiLWI1YjgtYTk5N2E4ZTAyY2U2In0.Hfq-QIJjSFCYNkABBXfbOuRjjlIhbFxHNWb7-upbHGcs7sIsyaNqnA4FbDKF9jiy_6rvm4-J1vD7pyHIA3W_7w")
                .execute()
                .body();
        log.info(result);
    }

    /**
     * 注释掉：@PreAuthorize("@ss.hasPermi('nursing:project:add')")和在SecurityConfig中加"/nursing/project/**"放行
     * <p>
     * 使用hutool中的HttpUtil工具类发起请求
     * URL： POST http://localhost:9000/nursing/project
     * <p>
     * 参数：
     * {
     * "image": "https://img2.baidu.com/it/u=3227619927,365499885&fm=253&fmt=auto&app=120&f=JPEG?w=938&h=500",
     * "name": "测试护理项目2",
     * "nursingRequirement": "测试护理项目2",
     * "orderNo": 1,
     * "price": 100,
     * "remark": "测试护理项目2",
     * "status": 1,
     * "unit": "次"
     * * }
     */
    @Test
    @DisplayName("2-POST简单请求")
    void postTest2() {
        // 构建bodyMap
        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("image", "https://img2.baidu.com/it/u=3227619927,365499885&fm=253&fmt=auto&app=120&f=JPEG?w=938&h=500");
        bodyMap.put("name", "测试护理项目AI2");
        bodyMap.put("nursingRequirement", "测试护理项目AI2");
        bodyMap.put("orderNo", 1);
        bodyMap.put("price", 100);
        bodyMap.put("remark", "测试护理项目AI2");
        bodyMap.put("status", 1);
        bodyMap.put("unit", "次");
        // 转换成json字符串
        String bodyStr = JSONUtil.toJsonStr(bodyMap);

        String result = HttpUtil.post("http://localhost:8080/nursing/project",bodyStr);
        log.info(result);
    }

}
