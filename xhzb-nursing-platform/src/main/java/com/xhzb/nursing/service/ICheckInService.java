package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.CheckIn;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.dto.checkIn.CheckInApplyDto;
import com.xhzb.nursing.domain.vo.checkIn.CheckInDetailVo;

/**
 * 入住管理Service接口
 * 
 * @author rippleflux
 * @date 2026-07-14
 */
public interface ICheckInService extends IService<CheckIn>
{
    /**
     * 查询入住管理
     * 
     * @param id 入住管理主键
     * @return 入住管理
     */
    public CheckIn selectCheckInById(Long id);

    /**
     * 查询入住管理列表
     * 
     * @param checkIn 入住管理
     * @return 入住管理集合
     */
    public List<CheckIn> selectCheckInList(CheckIn checkIn);

    /**
     * 新增入住管理
     * 
     * @param checkIn 入住管理
     * @return 结果
     */
    public int insertCheckIn(CheckIn checkIn);

    /**
     * 修改入住管理
     * 
     * @param checkIn 入住管理
     * @return 结果
     */
    public int updateCheckIn(CheckIn checkIn);

    /**
     * 批量删除入住管理
     * 
     * @param ids 需要删除的入住管理主键集合
     * @return 结果
     */
    public int deleteCheckInByIds(Long[] ids);

    /**
     * 删除入住管理信息
     * 
     * @param id 入住管理主键
     * @return 结果
     */
    public int deleteCheckInById(Long id);

    void apply(CheckInApplyDto dto);

    CheckInDetailVo detail(Long id);
}
