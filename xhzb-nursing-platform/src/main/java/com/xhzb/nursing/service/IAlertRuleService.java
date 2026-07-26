package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.AlertRule;
import com.xhzb.nursing.domain.DeviceData;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 报警规则Service接口
 * 
 * @author rippleflux
 * @date 2026-07-23
 */
public interface IAlertRuleService extends IService<AlertRule>
{
    /**
     * 查询报警规则
     * 
     * @param id 报警规则主键
     * @return 报警规则
     */
    public AlertRule selectAlertRuleById(Long id);

    /**
     * 查询报警规则列表
     * 
     * @param alertRule 报警规则
     * @return 报警规则集合
     */
    public List<AlertRule> selectAlertRuleList(AlertRule alertRule);

    /**
     * 新增报警规则
     * 
     * @param alertRule 报警规则
     * @return 结果
     */
    public int insertAlertRule(AlertRule alertRule);

    /**
     * 修改报警规则
     * 
     * @param alertRule 报警规则
     * @return 结果
     */
    public int updateAlertRule(AlertRule alertRule);

    /**
     * 批量删除报警规则
     * 
     * @param ids 需要删除的报警规则主键集合
     * @return 结果
     */
    public int deleteAlertRuleByIds(Long[] ids);

    /**
     * 删除报警规则信息
     * 
     * @param id 报警规则主键
     * @return 结果
     */
    public int deleteAlertRuleById(Long id);

    /**
     * 报警过滤：定时拉取设备数据，匹配启用规则，触发生成报警数据
     */
    void alertFilter();

    /**
     * 处理报警触发 — 由 Redis Pub/Sub 订阅者调用
     * 负责通知人查找 + 报警入库 + WebSocket 推送
     *
     * @param ruleId     报警规则 ID
     * @param deviceData 触发报警的设备数据
     */
    void handleAlertTrigger(Long ruleId, DeviceData deviceData);

    /**
     * 构建规则索引 — 将启用规则加载到 Redis
     * 应在应用启动 + 规则增删改后调用
     */
    void buildRuleIndex();
}
