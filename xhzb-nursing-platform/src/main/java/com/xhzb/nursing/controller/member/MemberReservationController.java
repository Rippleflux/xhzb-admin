package com.xhzb.nursing.controller.member;

import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import com.xhzb.common.core.domain.R;
import com.xhzb.common.core.page.TableDataInfo;
import com.xhzb.nursing.domain.Reservation;
import com.xhzb.nursing.domain.vo.TimeCountVO;
import com.xhzb.nursing.service.IReservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 预约信息Controller
 *
 * @author ruoyi
 */
@Slf4j
@RestController
@RequestMapping("/member/reservation")
@Tag(name = "预约信息相关接口")
public class MemberReservationController extends BaseController {
    @Autowired
    private IReservationService reservationService;


    @GetMapping("/cancelled-count")
    @Operation(summary = "查询取消预约数量")
    public R<Long> getCancelledReservationCount() {
        Long cancelledReservationCount = reservationService.getCancelledReservationCount();
        return R.ok(cancelledReservationCount);
    }

    @GetMapping("/countByTime")
    @Operation(summary = "查询每个时间段剩余预约次数")
    public R<List<TimeCountVO>> getCountByTime(@RequestParam("time") Long time) {
        List<TimeCountVO> countByTimeList = reservationService.getCountByTime(time);
        return R.ok(countByTimeList);
    }

    @PostMapping()
    @Operation(summary = "2.3 新增预约")
    public AjaxResult add(@RequestBody Reservation reservation) {
        return toAjax(reservationService.insertReservation(reservation));
    }

    @GetMapping("/page")
    @Operation(summary = "分页查询预约列表")
    public TableDataInfo list(Reservation reservation) {
        startPage();
        List<Reservation> list = reservationService.selectReservationList(reservation);
        return getDataTable(list);
    }


    /**
     * 取消预约信息
     */
    @PutMapping("/{id}/cancel")
    @Operation(summary = "删除预约信息")
    public AjaxResult cancel(@Schema(name = "预约信息ID", requiredMode = Schema.RequiredMode.REQUIRED) @PathVariable Long id) {
        return toAjax(reservationService.cancel(id));
    }
}

