package com.xhzb.nursing.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.ContractMapper;
import com.xhzb.nursing.domain.Contract;
import com.xhzb.nursing.service.IContractService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;

/**
 * 入住合同Service业务层处理
 * 
 * @author rippleflux
 * @date 2026-07-14
 */
@Service
public class ContractServiceImpl extends ServiceImpl<ContractMapper, Contract> implements IContractService
{
    @Autowired
    private ContractMapper contractMapper;

    /**
     * 查询入住合同
     * 
     * @param id 入住合同主键
     * @return 入住合同
     */
    @Override
    public Contract selectContractById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询入住合同列表
     * 
     * @param contract 入住合同
     * @return 入住合同
     */
    @Override
    public List<Contract> selectContractList(Contract contract)
    {
        return contractMapper.selectContractList(contract);
    }

    /**
     * 新增入住合同
     * 
     * @param contract 入住合同
     * @return 结果
     */
    @Override
    public int insertContract(Contract contract)
    {
        return save(contract)? 1 : 0;
    }

    /**
     * 修改入住合同
     * 
     * @param contract 入住合同
     * @return 结果
     */
    @Override
    public int updateContract(Contract contract)
    {
        return updateById(contract)? 1 : 0;
    }

    /**
     * 批量删除入住合同
     * 
     * @param ids 需要删除的入住合同主键
     * @return 结果
     */
    @Override
    public int deleteContractByIds(Long[] ids)
    {
        return removeByIds(Arrays.asList(ids))? 1 : 0;
    }

    /**
     * 删除入住合同信息
     * 
     * @param id 入住合同主键
     * @return 结果
     */
    @Override
    public int deleteContractById(Long id)
    {
        return removeById(id)? 1 : 0;
    }
}
