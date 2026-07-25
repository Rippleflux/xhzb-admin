package com.xhzb.nursing.service.impl;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.common.utils.UserThreadLocal;
import com.xhzb.nursing.domain.vo.TimeCountVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.ReservationMapper;
import com.xhzb.nursing.domain.Reservation;
import com.xhzb.nursing.service.IReservationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 预约信息Service业务层处理
 *
 * @author rippleflux
 * @date 2026-07-19
 */
@Service
public class ReservationServiceImpl extends ServiceImpl<ReservationMapper, Reservation> implements IReservationService {
    @Autowired
    private ReservationMapper reservationMapper;

    /**
     * 查询预约信息
     *
     * @param id 预约信息主键
     * @return 预约信息
     */
    @Override
    public Reservation selectReservationById(Long id) {
        return getById(id);
    }

    /**
     * 查询预约信息列表
     *
     * @param reservation 预约信息
     * @return 预约信息
     */
    @Override
    public List<Reservation> selectReservationList(Reservation reservation) {
        return reservationMapper.selectReservationList(reservation);
    }

    /**
     * 新增预约信息
     *
     * @param reservation 预约信息
     * @return 结果
     */
    @Override
    public int insertReservation(Reservation reservation) {
        String userId = String.valueOf(UserThreadLocal.getUserId());
        reservation.setStatus(0);
        reservation.setCreateBy(userId);
        return save(reservation) ? 1 : 0;
    }

    /**
     * 修改预约信息
     *
     * @param reservation 预约信息
     * @return 结果
     */
    @Override
    public int updateReservation(Reservation reservation) {
        return updateById(reservation) ? 1 : 0;
    }

    /**
     * 批量删除预约信息
     *
     * @param ids 需要删除的预约信息主键
     * @return 结果
     */
    @Override
    public int deleteReservationByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除预约信息信息
     *
     * @param id 预约信息主键
     * @return 结果
     */
    @Override
    public int deleteReservationById(Long id) {
        return removeById(id) ? 1 : 0;
    }



    @Override
    public int cancel(Long id) {
        Reservation reservation = reservationMapper.selectById(id);

        if (ObjectUtil.isEmpty(reservation) || reservation == null) {
            throw new ServiceException("当前预约信息不存在");
        }
        String userId = String.valueOf(UserThreadLocal.getUserId());
        reservation.setUpdateBy(userId);
        reservation.setStatus(2);
        return reservationMapper.updateById(reservation);
    }

    @Override
    public Long getCancelledReservationCount() {

        Long userId = UserThreadLocal.getUserId();

        // 获取今天的开始和结束时间
        LocalDateTime startOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Reservation::getUpdateBy, userId)          // update_by = 传入的id
                .between(Reservation::getUpdateTime, startOfDay, endOfDay) // update_time 在当天范围内
                .eq(Reservation::getStatus, 2);            // status = 2

        // 执行查询，返回符合条件的总记录数
        return reservationMapper.selectCount(wrapper);
    }

    @Override
    public List<TimeCountVO> getCountByTime(Long time) {
        LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochMilli(time), ZoneId.systemDefault())
                .with(LocalTime.MIN); // 当天 00:00:00
        LocalDateTime end = start.with(LocalTime.MAX); // 当天 23:59:59.999999999
        QueryWrapper<Reservation> wrapper = new QueryWrapper<>();
        wrapper.select("time, COUNT(*) as count")
                .ge("time", start)
                .lt("time", end)
                .groupBy("time");
        List<Map<String, Object>> list = reservationMapper.selectMaps(wrapper);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        List<TimeCountVO> voList = list.stream()
                .map(map -> {
                    TimeCountVO vo = new TimeCountVO();
                    // 如果 time 是 LocalDateTime
                    vo.setTime(((LocalDateTime) map.get("time")).format(formatter));
                    // 如果 time 是 Date，则用 SimpleDateFormat
                    // vo.setTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) map.get("time")));
                    vo.setCount(((Number) map.get("count")).intValue());
                    return vo;
                })
                .collect(Collectors.toList());

        return voList;
    }
}
