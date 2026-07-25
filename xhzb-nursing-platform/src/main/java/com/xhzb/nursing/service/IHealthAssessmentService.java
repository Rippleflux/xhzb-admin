package com.xhzb.nursing.service;

import java.util.List;
import java.util.Map;

import com.xhzb.nursing.domain.HealthAssessment;
import com.baomidou.mybatisplus.extension.service.IService;
import com.xhzb.nursing.domain.HealthAssessmentDataCollection;
import com.xhzb.nursing.domain.HealthAssessmentReport;
import com.xhzb.nursing.domain.dto.health.ElderAssessmentDto;

/**
 * 健康评估记录Service接口
 * 
 * @author rippleflux
 * @date 2026-07-13
 */
public interface IHealthAssessmentService extends IService<HealthAssessment>
{
    /**
     * 查询健康评估记录
     * 
     * @param id 健康评估记录主键
     * @return 健康评估记录
     */
    public HealthAssessmentDataCollection selectHealthAssessmentById(Long id);

    /**
     * 查询健康评估记录列表
     * 
     * @param healthAssessment 健康评估记录
     * @return 健康评估记录集合
     */
    public List<HealthAssessment> selectHealthAssessmentList(HealthAssessment healthAssessment);

    /**
     * 新增健康评估记录
     * 
     * @param data 健康评估记录
     * @return 结果
     */
    public Long insertHealthAssessment(ElderAssessmentDto data);

    /**
     * 修改健康评估记录
     * 
     * @param data 健康评估记录
     * @return 结果
     */
    public Long updateHealthAssessment(ElderAssessmentDto data);



    /**
     * 删除健康评估记录信息
     * 
     * @param id 健康评估记录主键
     * @return 结果
     */
    public Long deleteHealthAssessmentById(Long id);

    Long assessmentData(ElderAssessmentDto elderAssessmentDto);

    HealthAssessmentReport getAssessmentReportById(Long assessmentId);

    Long cancelHealthAssessment(Long assessmentId);

    Map<String, Object> getElderInfoByAssessmentId(Long assessmentId);
}
