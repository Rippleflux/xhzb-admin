package com.xhzb.nursing.service.impl;

import java.util.List;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.common.utils.UserThreadLocal;
import com.xhzb.nursing.domain.vo.MyFamilyPageVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.FamilyMemberElderMapper;
import com.xhzb.nursing.domain.FamilyMemberElder;
import com.xhzb.nursing.service.IFamilyMemberElderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;

/**
 * 客户老人关联Service业务层处理
 * 
 * @author rippleflux
 * @date 2026-07-22
 */
@Service
public class FamilyMemberElderServiceImpl extends ServiceImpl<FamilyMemberElderMapper, FamilyMemberElder> implements IFamilyMemberElderService
{
    @Autowired
    private FamilyMemberElderMapper familyMemberElderMapper;

    /**
     * 查询客户老人关联
     * 
     * @param id 客户老人关联主键
     * @return 客户老人关联
     */
    @Override
    public FamilyMemberElder selectFamilyMemberElderById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询客户老人关联列表
     * 
     * @param familyMemberElder 客户老人关联
     * @return 客户老人关联
     */
    @Override
    public List<FamilyMemberElder> selectFamilyMemberElderList(FamilyMemberElder familyMemberElder)
    {
        return familyMemberElderMapper.selectFamilyMemberElderList(familyMemberElder);
    }

    /**
     * 新增客户老人关联
     * 
     * @param familyMemberElder 客户老人关联
     * @return 结果
     */
    @Override
    public int insertFamilyMemberElder(FamilyMemberElder familyMemberElder)
    {
        return save(familyMemberElder)? 1 : 0;
    }

    /**
     * 修改客户老人关联
     * 
     * @param familyMemberElder 客户老人关联
     * @return 结果
     */
    @Override
    public int updateFamilyMemberElder(FamilyMemberElder familyMemberElder)
    {
        return updateById(familyMemberElder)? 1 : 0;
    }

    /**
     * 批量删除客户老人关联
     * 
     * @param ids 需要删除的客户老人关联主键
     * @return 结果
     */
    @Override
    public int deleteFamilyMemberElderByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除客户老人关联信息
     * 
     * @param id 客户老人关联主键
     * @return 结果
     */
    @Override
    public int deleteFamilyMemberElderById(Long id)
    {
        return removeById(id)? 1 : 0;
    }


    @Override
    public List<MyFamilyPageVo> getMyFamilyList( ) {
        Long userId = UserThreadLocal.getUserId();

        return familyMemberElderMapper.getMyFamilyList(userId);
    }
}
