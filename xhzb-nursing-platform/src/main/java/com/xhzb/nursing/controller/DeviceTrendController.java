package com.xhzb.nursing.controller;

import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.R;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.domain.vo.TrendPoint;
import com.xhzb.nursing.service.IInfluxDBService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 设备历史趋势接口
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@RestController
@RequestMapping("/elder/device")
@Tag(name = "设备历史趋势接口")
public class DeviceTrendController extends BaseController {

    @Autowired
    private IInfluxDBService influxDBService;

    /**
     * 查询设备历史趋势
     */
    @GetMapping("/{iotId}/trend")
    @PreAuthorize("@ss.hasPermi('elder:device:trend')")
    @Operation(summary = "查询设备历史趋势")
    public R<List<TrendPoint>> getTrend(
            @Schema(description = "设备ID", requiredMode = Schema.RequiredMode.REQUIRED)
            @PathVariable String iotId,
            @Schema(description = "物模型标识", requiredMode = Schema.RequiredMode.REQUIRED)
            @RequestParam String functionId,
            @Schema(description = "时间范围: 1h/24h/7d/30d", defaultValue = "24h")
            @RequestParam(defaultValue = "24h") String range,
            @Schema(description = "聚合间隔: 1m/5m/15m/1h", defaultValue = "5m")
            @RequestParam(defaultValue = "5m") String interval) {

        List<DeviceData> data = influxDBService.queryTrend(iotId, functionId, range, interval);
        List<TrendPoint> points = data.stream()
                .map(d -> TrendPoint.builder()
                        .time(d.getAlarmTime() != null ? d.getAlarmTime().toString() : "")
                        .value(parseDouble(d.getDataValue()))
                        .build())
                .collect(Collectors.toList());
        return R.ok(points);
    }

    /**
     * 查询设备最新 N 条数据
     */
    @GetMapping("/{iotId}/latest")
    @PreAuthorize("@ss.hasPermi('elder:device:latest')")
    @Operation(summary = "查询设备最新N条数据")
    public R<List<DeviceData>> getLatest(
            @Schema(description = "设备ID", requiredMode = Schema.RequiredMode.REQUIRED)
            @PathVariable String iotId,
            @Schema(description = "最大条数", defaultValue = "20")
            @RequestParam(defaultValue = "20") int limit) {

        return R.ok(influxDBService.queryLatest(iotId, limit));
    }

    /**
     * 查询设备指定时间段原始数据（无聚合，最大30天，默认1小时）
     */
    @GetMapping("/{iotId}/data")
    @PreAuthorize("@ss.hasPermi('elder:device:data')")
    @Operation(summary = "查询设备时间段原始数据")
    public R<List<DeviceData>> getData(
            @Schema(description = "设备ID", requiredMode = Schema.RequiredMode.REQUIRED)
            @PathVariable String iotId,
            @Schema(description = "物模型标识", requiredMode = Schema.RequiredMode.REQUIRED)
            @RequestParam String functionId,
            @Schema(description = "开始时间 (yyyy-MM-dd HH:mm:ss)")
            @RequestParam(required = false) String startTime,
            @Schema(description = "结束时间 (yyyy-MM-dd HH:mm:ss)")
            @RequestParam(required = false) String endTime) {

        LocalDateTime end = endTime != null
                ? LocalDateTime.parse(endTime.replace("T", " ").substring(0, 19),
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : LocalDateTime.now();
        LocalDateTime start = startTime != null
                ? LocalDateTime.parse(startTime.replace("T", " ").substring(0, 19),
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : end.minusHours(1);

        // 最大 30 天
        if (java.time.Duration.between(start, end).toDays() > 30) {
            start = end.minusDays(30);
        }

        return R.ok(influxDBService.queryRange(iotId, functionId, start, end));
    }

    private Double parseDouble(String val) {
        if (val == null || val.isEmpty()) return null;
        try { return Double.parseDouble(val); }
        catch (NumberFormatException e) { return null; }
    }
}
