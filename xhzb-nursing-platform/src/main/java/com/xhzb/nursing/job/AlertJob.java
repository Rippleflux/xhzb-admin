package com.xhzb.nursing.job;

import com.xhzb.nursing.service.IAlertRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 数据异常报警定时任务
 * 在后台管理中配置 cron: 0 * * * * ? (每分钟执行)
 *
 * @author rippleflux
 * @date 2026-07-23
 */
@Component
@Slf4j
public class AlertJob {

    @Autowired
    private IAlertRuleService alertRuleService;

    /**
     * 每分钟拉取设备数据并与报警规则比对
     * 由 RuoYi Quartz 调度框架调用
     */
    public void deviceDataAlertFilter() {
        log.debug("报警定时任务开始执行...");
        try {
            alertRuleService.alertFilter();
        } catch (Exception e) {
            log.error("报警定时任务执行异常", e);
        }
        log.debug("报警定时任务执行完成");
    }
}
