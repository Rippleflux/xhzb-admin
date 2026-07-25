package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.FamilyMemberElder;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.vo.MyFamilyPageVo;

/**
 * 客户老人关联Service接口
 * 
 * @author rippleflux
 * @date 2026-07-22
 */
public interface IFamilyMemberElderService extends IService<FamilyMemberElder>
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
     * 批量删除客户老人关联
     * 
     * @param ids 需要删除的客户老人关联主键集合
     * @return 结果
     */
    public int deleteFamilyMemberElderByIds(Long[] ids);

    /**
     * 删除客户老人关联信息
     * 
     * @param id 客户老人关联主键
     * @return 结果
     */
    public int deleteFamilyMemberElderById(Long id);

    List<MyFamilyPageVo> getMyFamilyList();

}
