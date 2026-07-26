package com.xhzb.common.constant;

/**
 * 缓存的key 常量
 * 
 * @author ruoyi
 */
public class CacheConstants
{
    /**
     * 登录用户 redis key
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 防重提交 redis key
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 限流 redis key
     */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";


    /**
     * 缓存所有产品列表的 key：
     */
    public static final String IOT_ALL_PRODUCT_LIST= "iot:all_product_list";

    /**
     * IoT设备最新上报数据缓存 key（Hash结构：field=iotId, value=设备数据JSON）
     */
    public static final String IOT_DEVICE_LAST_DATA = "iot:device_last_data";

    /**
     * 报警规则连续触发次数缓存前缀
     * Key: alert:trigger:count:{iotId}:{ruleId}
     */
    public static final String ALERT_TRIGGER_COUNT_PREFIX = "alert:trigger:count:";

    /**
     * 报警规则沉默周期缓存前缀
     * Key: alert:silence:cycle:{iotId}:{ruleId}
     */
    public static final String ALERT_SILENT_PREFIX = "alert:silence:cycle:";

    /**
     * 规则索引缓存前缀（按 functionId 索引规则 ID 列表）
     * Key: iot:rule:index:{functionId}
     * Type: Set
     */
    public static final String IOT_RULE_INDEX_PREFIX = "iot:rule:index:";

    /**
     * 规则详情缓存前缀（规则字段 Hash）
     * Key: iot:rule:cache:{ruleId}
     * Type: Hash
     */
    public static final String IOT_RULE_CACHE_PREFIX = "iot:rule:cache:";

    /**
     * 报警状态机缓存前缀
     * Key: iot:alert:state:{deviceId}:{ruleId}
     * Value: NORMAL / ALARM / RECOVERED / CLOSED
     */
    public static final String IOT_ALERT_STATE_PREFIX = "iot:alert:state:";

    /**
     * 消息幂等缓存前缀
     * Key: iot:message:processed:{deviceId}:{functionId}:{timestamp}
     * TTL: 1h
     */
    public static final String IOT_MESSAGE_PROCESSED = "iot:message:processed:";

    /**
     * 设备最后上报时间
     * Key: iot:device:last_report
     * Type: ZSet (deviceId → timestamp)
     */
    public static final String IOT_DEVICE_LAST_REPORT = "iot:device:last_report";
}
