package com.xhzb.nursing.enums;

/**
 * 报警状态机
 *
 * 状态转换规则:
 *   NORMAL ──(连续异常次数≥duration)──→ ALARM
 *   ALARM  ──(沉默周期结束)──────────→ NORMAL
 *   ALARM  ──(数据恢复正常)──────────→ RECOVERED
 *   RECOVERED ──(手动处理)───────────→ CLOSED
 *
 * @author rippleflux
 * @date 2026-07-26
 */
public enum AlertState {
    NORMAL("正常"),
    ALARM("报警中"),
    RECOVERED("已恢复"),
    CLOSED("已关闭");

    private final String desc;

    AlertState(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 判断是否可以从当前状态触发新报警
     */
    public boolean canTrigger() {
        return this == NORMAL || this == RECOVERED || this == CLOSED;
    }

    /**
     * 判断是否处于报警状态
     */
    public boolean isAlarming() {
        return this == ALARM;
    }
}
