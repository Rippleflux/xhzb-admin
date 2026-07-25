package com.xhzb.nursing.controller;

import java.util.List;

import com.xhzb.nursing.domain.dto.DeviceDto;
import com.xhzb.nursing.domain.vo.ProductVo;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.xhzb.common.annotation.Log;
import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.enums.BusinessType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.xhzb.nursing.domain.Device;
import com.xhzb.nursing.service.IDeviceService;
import com.xhzb.common.utils.poi.ExcelUtil;
import com.xhzb.common.core.page.TableDataInfo;

/**
 * 设备表Controller
 *
 * @author ripple
 * @date 2026-07-20
 */
@RestController
@RequestMapping("/nursing/device")
@Tag(name = "设备表相关接口")
public class DeviceController extends BaseController {
    @Autowired
    private IDeviceService deviceService;

    /**
     * 查询设备表列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:device:list')")
    @GetMapping("/list")
    @Operation(summary = "查询设备表列表")
    public TableDataInfo list(Device device) {
        startPage();
        List<Device> list = deviceService.selectDeviceList(device);
        return getDataTable(list);
    }

    /**
     * 同步产品数据列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:device:query')")
    @PostMapping("/syncProductList")
    @Operation(summary = "同步产品数据列表")
    public AjaxResult getSyncProductList() {
        deviceService.getSyncProductList();
        return success();
    }


    /**
     * 查询所有产品数据列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:device:query')")
    @GetMapping("/allProduct")
    @Operation(summary = "查询所有产品数据列表")
    public AjaxResult getAllProductList() {
        List<ProductVo> result = deviceService.getAllProductList();
        return success(result);
    }

    /**
     * 查询所有产品数据列表
     */
    @PostMapping("/register")
    @Operation(summary = "注册设备")
    public AjaxResult registerDevice(@RequestBody DeviceDto deviceDto) {
        deviceService.registerDevice(deviceDto);
        return success();
    }
    /**
     * 获取设备详细信息
     */
    @GetMapping("/{iotId}")
    @Operation(summary = "获取设备详细信息")
    public AjaxResult getInfo(@PathVariable("iotId") String iotId) {
        return success(deviceService.queryDeviceDetail(iotId));
    }

    @GetMapping("/queryServiceProperties/{iotId}")
    @Operation(summary = "查询设备上报数据")
    public AjaxResult queryServiceProperties(@PathVariable("iotId") String iotId) {
        AjaxResult ajaxResult = deviceService.queryServiceProperties(iotId);
        return ajaxResult;
    }


    /**
     * 导出设备表列表
     */
    @PreAuthorize("@ss.hasPermi('nursing:device:export')")
    @Log(title = "设备表", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    @Operation(summary = "导出设备表列表")
    public void export(HttpServletResponse response, Device device) {
        List<Device> list = deviceService.selectDeviceList(device);
        ExcelUtil<Device> util = new ExcelUtil<Device>(Device.class);
        util.exportExcel(response, list, "设备表数据");
    }

    ///**
    // * 获取设备表详细信息
    // */
    //@PreAuthorize("@ss.hasPermi('nursing:device:query')")
    //@GetMapping(value = "/{id}")
    //@Operation(summary = "获取设备表详细信息")
    //public AjaxResult getInfo(@Schema(name = "设备表ID", requiredMode = Schema.RequiredMode.REQUIRED)
    //                          @PathVariable("id") Long id) {
    //    return success(deviceService.selectDeviceById(id));
    //}

    /**
     * 新增设备表
     */
    @PreAuthorize("@ss.hasPermi('nursing:device:add')")
    @Log(title = "设备表", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "新增设备表")
    public AjaxResult add(@RequestBody Device device) {
        return toAjax(deviceService.insertDevice(device));
    }

    /**
     * 修改设备表
     */
    @PreAuthorize("@ss.hasPermi('nursing:device:edit')")
    @Log(title = "设备表", businessType = BusinessType.UPDATE)
    @PutMapping
    @Operation(summary = "修改设备表")
    public AjaxResult edit(@RequestBody Device device) {
        return toAjax(deviceService.updateDevice(device));
    }

    /**
     * 删除设备表
     */
    @PreAuthorize("@ss.hasPermi('nursing:device:remove')")
    @Log(title = "设备表", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    @Operation(summary = "删除设备表")
    public AjaxResult remove(@Schema(name = "设备表ID", requiredMode = Schema.RequiredMode.REQUIRED) @PathVariable Long[] ids) {
        return toAjax(deviceService.deleteDeviceByIds(ids));
    }

    @GetMapping("/queryProduct/{productKey}")
    @Operation(summary = "查询产品详情")
    public AjaxResult queryProduct(@PathVariable String productKey) {
        return deviceService.queryProduct(productKey);
    }
}
