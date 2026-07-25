# CLAUDE.md — 星海智伴后台管理系统 (xhzb-parent)

> 养老院管理平台，基于 RuoYi-Vue-Plus 脚手架，核心业务在 `xhzb-nursing-platform` 模块。

## 技术栈清单

| 分类 | 技术 | 版本 | 说明 |
|------|------|------|------|
| 语言 | Java | 17 | `-parameters` 编译 |
| 框架 | Spring Boot | 3.5.0 | |
| ORM | MyBatis-Plus | 3.5.7 | + MyBatis 3.5.16 |
| 分页 | PageHelper | 2.1.0 | `startPage()` / `getDataTable()` |
| 连接池 | Druid | 1.2.28 | `druid-spring-boot-3-starter` |
| 缓存 | Redis | — | `spring-boot-starter-data-redis` |
| 接口文档 | SpringDoc (OpenAPI 3) | 2.8.14 | `@Tag` / `@Operation` / `@Schema` |
| JSON | Fastjson2 | 2.0.57 | + Hutool JSONUtil |
| 工具 | Hutool | — | `cn.hutool.*` |
| 安全 | Spring Security | — | `@PreAuthorize("@ss.hasPermi(...)")` |
| 认证 | JWT | 0.9.1 | `token.header: Authorization` |
| 定时任务 | Quartz | — | `xhzb-quartz` 模块 |
| AI | Spring AI | 1.1.2 | OpenAI / DeepSeek / Ollama |
| IoT | HuaweiCloud IoTDA | — | AMQP + 设备影子 |
| Excel | Apache POI | 4.1.2 | `@Excel` 注解导出 |
| 构建 | Maven | 3.14.0 | 多模块 pom |

## 模块目录

```
xhzb-parent/
├── xhzb-admin/             # 启动模块（main 入口 + application.yml）
├── xhzb-common/            # 通用工具、常量、注解（CacheConstants 等）
├── xhzb-framework/         # 框架层（Security、Config、拦截器）
├── xhzb-system/            # 系统管理（用户、角色、菜单、字典）
├── xhzb-quartz/            # 定时任务管理
├── xhzb-generator/         # 代码生成器
├── xhzb-oss/               # 对象存储
└── xhzb-nursing-platform/  # ★ 核心业务模块（养老护工平台）
    └── src/main/java/com/xhzb/nursing/
        ├── config/         # Spring AI / Redis 配置
        ├── constants/      # 业务常量（SystemConstants）
        ├── controller/     # REST 控制器
        ├── domain/         # 实体类 + VO + DTO
        ├── enums/          # 业务枚举
        ├── job/            # AMQP 消息消费（AmqpClient）
        ├── mapper/         # MyBatis Mapper 接口
        ├── service/        # Service 接口 + impl
        ├── tools/          # AI Tool（WeatherTools 等）
        └── util/           # 工具类（DateTimeZoneConverter 等）
    └── src/main/resources/mapper/nursing/
        └── *Mapper.xml     # MyBatis SQL 映射
```

## Controller 代码模板

```java
package com.xhzb.nursing.controller;

import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.core.domain.R;
import com.xhzb.common.core.page.TableDataInfo;
import com.xhzb.nursing.domain.Bed;                     // 实体类
import com.xhzb.nursing.domain.vo.BedVo;               // VO（如有）
import com.xhzb.nursing.service.IBedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/elder/bed")                           // 路径前缀 /elder/xxx
@Tag(name = "床位相关接口")                             // Swagger 分组名
public class BedController extends BaseController {

    @Autowired
    private IBedService bedService;

    // --- 分页列表 ---
    @PreAuthorize("@ss.hasPermi('elder:bed:list')")    // 权限控制
    @GetMapping("/list")
    @Operation(summary = "查询床位列表")
    public TableDataInfo list(Bed bed) {               // 参数直接接实体
        startPage();                                   // PageHelper 分页
        List<Bed> list = bedService.selectBedList(bed);
        return getDataTable(list);
    }

    // --- 详情 ---
    @PreAuthorize("@ss.hasPermi('elder:bed:query')")
    @GetMapping("/{id}")
    @Operation(summary = "获取床位详细信息")
    public R<Bed> getInfo(
        @Schema(name = "床位ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @PathVariable("id") Long id) {
        return R.ok(bedService.selectBedById(id));     // 单条用 R<T>
    }

    // --- 新增 ---
    @PreAuthorize("@ss.hasPermi('elder:bed:add')")
    @Log(title = "床位", businessType = BusinessType.INSERT)
    @PostMapping
    @Operation(summary = "新增床位")
    public AjaxResult add(@RequestBody Bed bed) {
        return toAjax(bedService.insertBed(bed));      // 增删改用 AjaxResult
    }

    // --- 自定义查询（无权限控制的列表接口）---
    @GetMapping("/getRoomsWithDeviceByFloorId/{floorId}")
    @Operation(summary = "获取房间中的智能设备及数据")
    public R<List<RoomVo>> getRoomsWithDeviceByFloorId(
        @Schema(name = "楼层ID", requiredMode = Schema.RequiredMode.REQUIRED)
        @PathVariable("floorId") Long floorId) {
        return R.ok(roomService.getRoomsWithDeviceByFloorId(floorId));
    }
}
```

## Service 代码模板

```java
package com.xhzb.nursing.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xhzb.nursing.domain.Bed;
import com.xhzb.nursing.mapper.BedMapper;
import com.xhzb.nursing.service.IBedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class BedServiceImpl
        extends ServiceImpl<BedMapper, Bed>             // MyBatis-Plus 基类
        implements IBedService {

    @Autowired
    private BedMapper bedMapper;                       // 自定义 SQL 用 Mapper

    @Override
    public Bed selectBedById(Long id) {
        return getById(id);                            // MyBatis-Plus 内置方法
    }

    @Override
    public List<Bed> selectBedList(Bed bed) {
        return bedMapper.selectBedList(bed);           // 自定义 Mapper XML
    }

    @Override
    public int insertBed(Bed bed) {
        return save(bed) ? 1 : 0;                     // MyBatis-Plus 内置
    }

    @Override
    public int updateBed(Bed bed) {
        return updateById(bed) ? 1 : 0;
    }

    @Override
    public int deleteBedByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }
}
```

### 返回值约定

| 场景 | 返回类型 | 示例 |
|------|---------|------|
| 单条数据 | `R<T>` | `return R.ok(bed);` |
| 列表数据 | `R<List<T>>` | `return R.ok(list);` |
| 增删改 | `AjaxResult` | `return toAjax(rows)` / `return success()` |
| 分页 | `TableDataInfo` | `startPage(); return getDataTable(list);` |
| 无数据成功 | `AjaxResult` | `return success();` |

## ✅ 必须做的 3 条

1. **Controller 继承 `BaseController`，Service 继承 `ServiceImpl<Mapper, Entity>`** — 这是项目约定，不要另起炉灶
2. **自定义 SQL 写 Mapper XML，简单 CRUD 用 MyBatis-Plus 内置方法** — `getById()` / `save()` / `updateById()` / `removeByIds()` / `lambdaQuery()`
3. **接口路径前缀用 `/elder/xxx`，注解加满 Swagger** — 每个公开方法必须有 `@Operation(summary = "...")`，路径参数加 `@Schema(requiredMode = REQUIRED)`

## ❌ 禁止做的 3 条

1. **禁止在 Controller 写业务逻辑或拼 SQL** — Controller 只做参数接收→调用 Service→包装返回，一切逻辑下沉到 ServiceImpl
2. **禁止硬编码常量** — 缓存 key 放 `CacheConstants`，业务常量放 `SystemConstants`，配置放 `application.yml`
3. **禁止返回裸 `List` 或 `String`** — 必须用 `R<T>` / `AjaxResult` / `TableDataInfo` 统一包装，走框架的 `BaseController.success()` / `R.ok()` 方法
