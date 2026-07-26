package com.xhzb.nursing.service.impl;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.xhzb.nursing.domain.DeviceData;
import com.xhzb.nursing.service.IInfluxDBService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * InfluxDB 2.7 时序数据服务实现
 *
 * @author rippleflux
 * @date 2026-07-26
 */
@Slf4j
@Service
public class InfluxDBServiceImpl implements IInfluxDBService {

    @Autowired
    private InfluxDBClient influxDBClient;

    @Override
    public void writeDeviceData(List<DeviceData> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        final int maxRetries = 3;
        final long retryDelayMs = 1000;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            try {
                WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
                List<Point> points = new ArrayList<>();
                for (DeviceData d : list) {
                    Point point = Point.measurement("device_data")
                            .addTag("iot_id", d.getIotId())
                            .addTag("product_key", d.getProductKey())
                            .addTag("function_id", d.getFunctionId())
                            .addTag("device_name", d.getDeviceName())
                            .addField("data_value", parseDouble(d.getDataValue()))
                            .addField("product_name", nvl(d.getProductName()))
                            .addField("location_type", nvl(d.getLocationType(), 0))
                            .addField("physical_location_type", nvl(d.getPhysicalLocationType(), 0))
                            .addField("access_location", nvl(d.getAccessLocation()))
                            .addField("device_description", nvl(d.getDeviceDescription()))
                            .time(toEpochMilli(d.getAlarmTime()), WritePrecision.MS);
                    points.add(point);
                }
                writeApi.writePoints(points);
                return; // 成功则返回
            } catch (Exception e) {
                if (attempt < maxRetries - 1) {
                    log.warn("InfluxDB 写入失败(第{}次重试), count={}", attempt + 1, list.size());
                    try { Thread.sleep(retryDelayMs); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                } else {
                    log.error("InfluxDB 写入失败(已达最大重试{}次), count={}", maxRetries, list.size(), e);
                }
            }
        }
    }

    @Override
    public List<DeviceData> queryLatest(String iotId, int limit) {
        String flux = String.format(
                "from(bucket: \"nursing\") " +
                "|> range(start: -30d) " +
                "|> filter(fn: (r) => r[\"_measurement\"] == \"device_data\") " +
                "|> filter(fn: (r) => r[\"iot_id\"] == \"%s\") " +
                "|> pivot(rowKey:[\"_time\"], columnKey:[\"_field\"], valueColumn:\"_value\") " +
                "|> sort(columns: [\"_time\"], desc: true) " +
                "|> limit(n: %d)", iotId, limit);
        return query(flux);
    }

    @Override
    public List<DeviceData> queryRange(String iotId, String functionId,
                                        LocalDateTime start, LocalDateTime end) {
        String flux = String.format(
                "from(bucket: \"nursing\") " +
                "|> range(start: %s, stop: %s) " +
                "|> filter(fn: (r) => r[\"_measurement\"] == \"device_data\") " +
                "|> filter(fn: (r) => r[\"iot_id\"] == \"%s\") " +
                "|> filter(fn: (r) => r[\"function_id\"] == \"%s\") " +
                "|> pivot(rowKey:[\"_time\"], columnKey:[\"_field\"], valueColumn:\"_value\") " +
                "|> sort(columns: [\"_time\"], desc: false)",
                toFluxTime(start), toFluxTime(end), iotId, functionId);
        return query(flux);
    }

    @Override
    public List<DeviceData> queryTrend(String iotId, String functionId,
                                        String range, String interval) {
        String rangeExpr = parseRangeExpr(range);
        String flux = String.format(
                "from(bucket: \"nursing\") " +
                "|> range(start: %s) " +
                "|> filter(fn: (r) => r[\"_measurement\"] == \"device_data\") " +
                "|> filter(fn: (r) => r[\"iot_id\"] == \"%s\") " +
                "|> filter(fn: (r) => r[\"function_id\"] == \"%s\") " +
                "|> filter(fn: (r) => r[\"_field\"] == \"data_value\") " +
                "|> aggregateWindow(every: %s, fn: mean, createEmpty: false) " +
                "|> sort(columns: [\"_time\"], desc: false)",
                rangeExpr, iotId, functionId, interval);
        return query(flux);
    }

    // ═══════════════════════════════════════════════════════
    //  Flux 查询 → DeviceData 转换
    // ═══════════════════════════════════════════════════════

    private List<DeviceData> query(String flux) {
        List<DeviceData> result = new ArrayList<>();
        try {
            List<FluxTable> tables = influxDBClient.getQueryApi().query(flux);
            for (FluxTable table : tables) {
                for (FluxRecord record : table.getRecords()) {
                    DeviceData d = new DeviceData();
                    d.setIotId((String) record.getValueByKey("iot_id"));
                    d.setProductKey((String) record.getValueByKey("product_key"));
                    d.setFunctionId((String) record.getValueByKey("function_id"));
                    d.setDeviceName((String) record.getValueByKey("device_name"));
                    Object val = record.getValueByKey("data_value");
                    d.setDataValue(val != null ? val.toString() : null);
                    Object pn = record.getValueByKey("product_name");
                    d.setProductName(pn != null ? pn.toString() : null);
                    Object lt = record.getValueByKey("location_type");
                    d.setLocationType(lt != null ? ((Number) lt).intValue() : null);
                    Object plt = record.getValueByKey("physical_location_type");
                    d.setPhysicalLocationType(plt != null ? ((Number) plt).intValue() : null);
                    d.setAccessLocation((String) record.getValueByKey("access_location"));
                    d.setDeviceDescription((String) record.getValueByKey("device_description"));
                    if (record.getTime() != null) {
                        d.setAlarmTime(LocalDateTime.ofInstant(record.getTime(), ZoneId.of("Asia/Shanghai")));
                    }
                    result.add(d);
                }
            }
        } catch (Exception e) {
            log.error("InfluxDB 查询失败", e);
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════
    //  工具方法
    // ═══════════════════════════════════════════════════════

    private double parseDouble(String val) {
        if (val == null || val.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(val);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private String nvl(String s) { return s != null ? s : ""; }
    private int nvl(Integer i, int def) { return i != null ? i : def; }

    private long toEpochMilli(LocalDateTime dt) {
        if (dt == null) return System.currentTimeMillis();
        return dt.atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
    }

    private String toFluxTime(LocalDateTime dt) {
        return dt.atZone(ZoneId.of("Asia/Shanghai")).toInstant().toString();
    }

    private String parseRangeExpr(String range) {
        return switch (range) {
            case "1h" -> "-1h";
            case "7d" -> "-7d";
            case "30d" -> "-30d";
            default -> "-24h";
        };
    }
}
