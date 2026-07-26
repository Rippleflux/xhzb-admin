package com.xhzb.nursing.domain.event;

import com.xhzb.nursing.domain.Device;
import com.xhzb.nursing.domain.DeviceData;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 设备数据接收事件 — 用于 Spring Event 解耦
 *
 * @author rippleflux
 * @date 2026-07-26
 */
public class DeviceDataEvent extends ApplicationEvent {

    private final List<DeviceData> deviceDataList;
    private final Device device;

    public DeviceDataEvent(Object source, List<DeviceData> deviceDataList, Device device) {
        super(source);
        this.deviceDataList = deviceDataList;
        this.device = device;
    }

    public List<DeviceData> getDeviceDataList() {
        return deviceDataList;
    }

    public Device getDevice() {
        return device;
    }
}
