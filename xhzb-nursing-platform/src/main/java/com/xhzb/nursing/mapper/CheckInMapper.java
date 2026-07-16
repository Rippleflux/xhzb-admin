package com.xhzb.nursing.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.nursing.domain.CheckIn;

/**
 * 入住管理Mapper接口
 * 
 * @author rippleflux
 * @date 2026-07-14
 */
@Mapper
public interface CheckInMapper extends BaseMapper<CheckIn>
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
     * 删除入住管理
     * 
     * @param id 入住管理主键
     * @return 结果
     */
    public int deleteCheckInById(Long id);

    /**
     * 批量删除入住管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteCheckInByIds(Long[] ids);
}
