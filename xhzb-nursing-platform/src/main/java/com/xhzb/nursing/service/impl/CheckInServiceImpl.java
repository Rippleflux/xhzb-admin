package com.xhzb.nursing.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.common.utils.StringUtils;
import com.xhzb.nursing.domain.*;
import com.xhzb.nursing.domain.dto.checkIn.*;
import com.xhzb.nursing.domain.vo.checkIn.CheckInConfigVo;
import com.xhzb.nursing.domain.vo.checkIn.CheckInDetailVo;
import com.xhzb.nursing.domain.vo.checkIn.CheckInElderVo;
import com.xhzb.nursing.domain.vo.checkIn.ElderFamilyVo;
import com.xhzb.nursing.mapper.*;
import com.xhzb.nursing.util.CodeGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.service.ICheckInService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 入住管理Service业务层处理
 *
 * @author rippleflux
 * @date 2026-07-14
 */
@Service
public class CheckInServiceImpl extends ServiceImpl<CheckInMapper, CheckIn> implements ICheckInService {
    @Autowired
    CheckInMapper checkInMapper;
    @Autowired
    HealthAssessmentMapper healthAssessmentMapper;
    @Autowired
    BedMapper bedMapper;

    @Autowired
    ElderMapper elderMapper;

    @Autowired
    ContractMapper contractMapper;

    @Autowired
    CheckInConfigMapper checkInConfigMapper;

    /**
     * 查询入住管理
     *
     * @param id 入住管理主键
     * @return 入住管理
     */
    @Override
    public CheckIn selectCheckInById(Long id) {
        return getById(id);
    }

    /**
     * 查询入住管理列表
     *
     * @param checkIn 入住管理
     * @return 入住管理
     */
    @Override
    public List<CheckIn> selectCheckInList(CheckIn checkIn) {
        return checkInMapper.selectCheckInList(checkIn);
    }

    /**
     * 新增入住管理
     *
     * @param checkIn 入住管理
     * @return 结果
     */
    @Override
    public int insertCheckIn(CheckIn checkIn) {
        return save(checkIn) ? 1 : 0;
    }

    /**
     * 修改入住管理
     *
     * @param checkIn 入住管理
     * @return 结果
     */
    @Override
    public int updateCheckIn(CheckIn checkIn) {
        return updateById(checkIn) ? 1 : 0;
    }

    /**
     * 批量删除入住管理
     *
     * @param ids 需要删除的入住管理主键
     * @return 结果
     */
    @Override
    public int deleteCheckInByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除入住管理信息
     *
     * @param id 入住管理主键
     * @return 结果
     */
    @Override
    public int deleteCheckInById(Long id) {
        return removeById(id) ? 1 : 0;
    }


    /**
     * 入住申请
     *
     * @param data
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void apply(CheckInApplyDto data) {
        // 1. 查询健康评估的详细信息,判断健康评估是否完成
        Long healthAssessmentId = data.getHealthAssessmentId();

        HealthAssessment healthAssessment = healthAssessmentMapper.selectById(healthAssessmentId);

        if (healthAssessment == null) {
            throw new ServiceException("健康评估不存在");
        }
        if (healthAssessment.getEvaluationProgress() != 1) {
            throw new ServiceException("健康评估未完成,请先完成健康评估");
        }

        Bed bed = updateBedStatus(data);

        // 3. 新增或者更新老人信息
        Elder elder = saveOrUpdateElder(data, bed, healthAssessment);

        // 5. 新增签约办理信息
        Contract contract = insertContract(data, elder);

        // 6. 新增入住信息
        CheckIn checkIn = insertCheckIn(data, elder, contract, bed);

        // 7. 新增入住配置信息
        CheckInConfig checkInConfig = insertCheckInConfig(data, checkIn);
        // 8. 更新健康评估数据
        healthAssessment.setElderId(elder.getId());
        healthAssessment.setCheckInStatus(1);
        healthAssessmentMapper.updateById(healthAssessment);
    }

    @Override
    public CheckInDetailVo detail(Long id) {
        //Contract

        CheckInDetailVo checkInDetailVo = new CheckInDetailVo();
        CheckIn checkIn = checkInMapper.selectById(id);

        LambdaQueryWrapper<Contract> queryWrapper = Wrappers.lambdaQuery(Contract.class).eq(Contract::getElderId, checkIn.getElderId());

        Contract contract = contractMapper.selectOne(queryWrapper);

        CheckInConfigVo checkInConfigVo = BeanUtil.copyProperties(checkIn, CheckInConfigVo.class);
        Elder elder = elderMapper.selectById(checkIn.getElderId());
        CheckInElderVo checkInElderVo = BeanUtil.copyProperties(elder, CheckInElderVo.class);
        checkInDetailVo.setCheckInElderVo(checkInElderVo);
        checkInDetailVo.setCheckInConfigVo(checkInConfigVo);
        checkInDetailVo.setContract(contract);
        //
        if (!StringUtils.isEmpty(checkIn.getRemark())) {
            JSONArray entries = JSONUtil.parseArray(checkIn.getRemark());
            List<ElderFamilyVo> collect = entries.stream().map(item -> BeanUtil.copyProperties(item, ElderFamilyVo.class)).toList();
            checkInDetailVo.setElderFamilyVoList(collect);
        }

        return checkInDetailVo;

    }

    // 新增入住配置信息
    private CheckInConfig insertCheckInConfig(CheckInApplyDto checkInApplyDto, CheckIn checkIn) {
        CheckInConfigDto checkInConfigDto = checkInApplyDto.getCheckInConfigDto();
        CheckInConfig checkInConfig = BeanUtil.copyProperties(checkInConfigDto, CheckInConfig.class);
        checkInConfig.setCheckInId(checkIn.getId());
        checkInConfigMapper.insert(checkInConfig);

        return checkInConfig;
    }

    // 新增入住信息
    private CheckIn insertCheckIn(CheckInApplyDto checkInApplyDto, Elder elder, Contract contract, Bed bed) {
        CheckIn checkIn = new CheckIn();
        checkIn.setElderId(elder.getId());
        checkIn.setElderName(elder.getName());
        checkIn.setIdCardNo(elder.getIdCardNo());
        checkIn.setStartDate(contract.getStartDate());
        checkIn.setEndDate(contract.getEndDate());

        CheckInConfigDto checkInConfigDto = checkInApplyDto.getCheckInConfigDto();
        // 护理等级名称
        checkIn.setNursingLevelName(checkInConfigDto.getNursingLevelName());
        // 床位编号
        checkIn.setBedNumber(bed.getBedNumber());
        checkIn.setStatus(0);
        // 将老人家属信息转化为JSON字符串存到入住表中
        List<ElderFamilyDto> elderFamilyDtoList = checkInApplyDto.getElderFamilyDtoList();
        checkIn.setRemark(JSONUtil.toJsonStr(elderFamilyDtoList));

        checkInMapper.insert(checkIn);

        return checkIn;
    }

    // 新增或者更新老人信息
    private Elder saveOrUpdateElder(CheckInApplyDto checkInApplyDto, Bed bed, HealthAssessment healthAssessment) {
        CheckInElderDto checkInElderDto = checkInApplyDto.getCheckInElderDto();
        if (checkInApplyDto == null) {
            throw new ServiceException("请填写老人信息");
        }
        String idCardNo = checkInElderDto.getIdCardNo();
        Elder elder = elderMapper.selectOne(Wrappers.lambdaQuery(Elder.class).eq(Elder::getIdCardNo, idCardNo));
        if (elder != null && elder.getStatus() != 3) {
            throw new ServiceException("老人状态异常,请检查老人数据状态");
        }
        // 4. 新增或者更新老人信息
        // elder == null     elder.getStatus() == 3
        if (elder == null) {
            elder = new Elder();
        }
        // 将最新的老人信息设置到老人对象中(新增/修改)
        BeanUtil.copyProperties(checkInElderDto, elder, CopyOptions.create().ignoreNullValue());
        elder.setBedId(bed.getId());
        elder.setBedNumber(bed.getBedNumber());
        elder.setCoreSuggestion(healthAssessment.getCoreSuggestion() == 0 ? "不建议入住" : "建议入住");
        // 新增或者更新老人信息
        elderMapper.insertOrUpdate(elder);

        return elder;
    }

    private Contract insertContract(CheckInApplyDto checkInApplyDto, Elder elder) {
        CheckInContractDto checkInContractDto = checkInApplyDto.getCheckInContractDto();
        if (checkInContractDto == null) {
            throw new ServiceException("请填写签约信息");
        }
        Contract contract = BeanUtil.copyProperties(checkInContractDto, Contract.class);
        contract.setElderId(elder.getId());
        contract.setElderName(elder.getName());
        // 生成16位协议编号
        contract.setContractNumber(CodeGenerator.generateContractNumber());

        CheckInConfigDto checkInConfigDto = checkInApplyDto.getCheckInConfigDto();
        contract.setStartDate(checkInConfigDto.getStartDate());
        contract.setEndDate(checkInConfigDto.getEndDate());
        contract.setSignDate(LocalDateTime.now());
        contract.setStatus(contract.getStartDate().isBefore(LocalDateTime.now()) ? 1 : 0);

        // 新增签约办理信息
        contractMapper.insert(contract);

        return contract;
    }

    private Bed updateBedStatus(CheckInApplyDto data) {
        CheckInConfigDto checkInConfigDto = data.getCheckInConfigDto();

        if (checkInConfigDto == null) {
            throw new ServiceException("请填写入住配置信息");
        }

        Bed bed = bedMapper.selectById(checkInConfigDto.getBedId());
        if (bed == null || bed.getBedStatus() != 0) {
            throw new ServiceException("床位不存在或者已入住");
        }

        bed.setBedStatus(1); // 设置床位状态为已入住
        bedMapper.updateById(bed);

        return bed;
    }
}
