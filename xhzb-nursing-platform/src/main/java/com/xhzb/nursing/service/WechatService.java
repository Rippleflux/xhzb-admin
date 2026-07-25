package com.xhzb.nursing.service;

/**
 * 微信接口调用业务层封装
 */
public interface WechatService {
    /**
     * 获取openid
     *
     * @param code
     * @return
     */
    String getOpenid(String code);

    /**
     * 获取手机号
     *
     * @param detailCode
     * @return
     */
    String getPhone(String detailCode);

    /**
     * 获取接口调用凭证
     *
     */
    String getAccessToken();

}