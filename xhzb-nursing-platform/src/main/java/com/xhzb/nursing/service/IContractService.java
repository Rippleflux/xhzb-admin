package com.xhzb.nursing.service;

import java.util.List;
import com.xhzb.nursing.domain.Contract;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * 入住合同Service接口
 * 
 * @author rippleflux
 * @date 2026-07-14
 */
public interface IContractService extends IService<Contract>
{
    /**
     * 查询入住合同
     * 
     * @param id 入住合同主键
     * @return 入住合同
     */
    public Contract selectContractById(Long id);

    /**
     * 查询入住合同列表
     * 
     * @param contract 入住合同
     * @return 入住合同集合
     */
    public List<Contract> selectContractList(Contract contract);

    /**
     * 新增入住合同
     * 
     * @param contract 入住合同
     * @return 结果
     */
    public int insertContract(Contract contract);

    /**
     * 修改入住合同
     * 
     * @param contract 入住合同
     * @return 结果
     */
    public int updateContract(Contract contract);

    /**
     * 批量删除入住合同
     * 
     * @param ids 需要删除的入住合同主键集合
     * @return 结果
     */
    public int deleteContractByIds(Long[] ids);

    /**
     * 删除入住合同信息
     * 
     * @param id 入住合同主键
     * @return 结果
     */
    public int deleteContractById(Long id);
}
