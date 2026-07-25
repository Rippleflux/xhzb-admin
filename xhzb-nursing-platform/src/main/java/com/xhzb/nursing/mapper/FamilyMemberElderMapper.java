package com.xhzb.nursing.mapper;

import java.util.List;

import com.xhzb.nursing.domain.vo.MyFamilyPageVo;
import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.nursing.domain.FamilyMemberElder;

/**
 * 客户老人关联Mapper接口
 * 
 * @author rippleflux
 * @date 2026-07-22
 */
@Mapper
public interface FamilyMemberElderMapper extends BaseMapper<FamilyMemberElder>
{
    /**
     * 查询客户老人关联
     * 
     * @param id 客户老人关联主键
     * @return 客户老人关联
     */
    public FamilyMemberElder selectFamilyMemberElderById(Long id);

    /**
     * 查询客户老人关联列表
     * 
     * @param familyMemberElder 客户老人关联
     * @return 客户老人关联集合
     */
    public List<FamilyMemberElder> selectFamilyMemberElderList(FamilyMemberElder familyMemberElder);

    /**
     * 新增客户老人关联
     * 
     * @param familyMemberElder 客户老人关联
     * @return 结果
     */
    public int insertFamilyMemberElder(FamilyMemberElder familyMemberElder);

    /**
     * 修改客户老人关联
     * 
     * @param familyMemberElder 客户老人关联
     * @return 结果
     */
    public int updateFamilyMemberElder(FamilyMemberElder familyMemberElder);

    /**
     * 删除客户老人关联
     * 
     * @param id 客户老人关联主键
     * @return 结果
     */
    public int deleteFamilyMemberElderById(Long id);

    /**
     * 批量删除客户老人关联
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFamilyMemberElderByIds(Long[] ids);

    List<MyFamilyPageVo> getMyFamilyList(Long userId);

}
