package com.xhzb.nursing.service.impl;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.xhzb.nursing.service.WechatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class WechatServiceImpl implements WechatService {

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    @Override
    public String getOpenid(String code) {
        Map<String, Object> queryParams = MapUtil.<String, Object>builder()
                .put("appid", appid)
                .put("secret", secret)
                .put("js_code", code)
                .put("grant_type", "authorization_code")
                .build();
        // 2-请求：
        String code2Session_url = "https://api.weixin.qq.com/sns/jscode2session";
        String jsonResult = HttpUtil.get(code2Session_url, queryParams);
        // 3-响应：解析
        JSONObject result = JSONUtil.parseObj(jsonResult);
        if (ObjectUtil.isNotNull(result.getInt("errcode"))) {
            throw new RuntimeException(result.getStr("errmsg"));
        }
        return result.getStr("openid");
    }

    @Override
    public String getPhone(String detailCode) {
        // POST https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=ACCESS_TOKEN

        String accessToken = getAccessToken();

        Map<String, Object> data = MapUtil.<String, Object>builder()
                .put("code", detailCode)
                .build();

        String get_user_phone_number_url = "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + accessToken;


        String result_data = HttpUtil.createPost(get_user_phone_number_url)
                .body(JSONUtil.toJsonStr(data))
                .execute()
                .body();


        JSONObject result = JSONUtil.parseObj(result_data);

        if (result.getInt("errcode") != 0) {
            throw new RuntimeException(result.getStr("errmsg"));
        }

        //return result.getObj("phone_info").toString();
        JSONObject phoneInfo = result.getJSONObject("phone_info");
        String phoneNumber = phoneInfo.getStr("phoneNumber");
        return phoneNumber;
    }

    @Override
    public String getAccessToken() {
        //GET https://api.weixin.qq.com/cgi-bin/token?appid=AppID&secret=AppSecret&grant_type=client_credential

        Map<String, Object> queryParams = MapUtil.<String, Object>builder()
                .put("appid", appid)
                .put("secret", secret)
                .put("grant_type", "client_credential")
                .build();
        // 2-请求：
        String get_token_url = "https://api.weixin.qq.com/cgi-bin/token";
        String jsonResult = HttpUtil.get(get_token_url, queryParams);

        log.info("-----------------{}", jsonResult);

        // 3-响应：解析
        JSONObject result = JSONUtil.parseObj(jsonResult);

        log.info("-----------------{}", result.getStr("access_token"));

        if (ObjectUtil.isNotNull(result.getStr("access_token"))) {
            return result.getStr("access_token");
        }
        throw new RuntimeException("获取token失败");

    }


}
