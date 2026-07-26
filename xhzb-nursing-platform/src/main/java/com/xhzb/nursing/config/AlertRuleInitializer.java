package com.xhzb.nursing.config;

import com.xhzb.nursing.service.IAlertRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 应用启动初始化器 — 报警规则索引预热
 * <p>
 * 应用启动时将启用的报警规则加载到 Redis 规则缓存 + 规则索引，
 * 确保 AlertDetectionListener 能立即开始事件驱动检测。
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertRuleInitializer implements ApplicationRunner {

    private final IAlertRuleService alertRuleService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("开始构建报警规则索引...");
        try {
            alertRuleService.buildRuleIndex();
            log.info("报警规则索引构建完成");
        } catch (Exception e) {
            log.error("报警规则索引构建失败", e);
        }
    }
}
