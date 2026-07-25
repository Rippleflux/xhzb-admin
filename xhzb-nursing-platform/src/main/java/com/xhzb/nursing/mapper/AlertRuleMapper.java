package com.xhzb.nursing.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.nursing.domain.AlertRule;

/**
 * 报警规则Mapper接口
 * 
 * @author rippleflux
 * @date 2026-07-23
 */
@Mapper
public interface AlertRuleMapper extends BaseMapper<AlertRule>
{
    /**
     * 查询报警规则
     * 
     * @param id 报警规则主键
     * @return 报警规则
     */
    public AlertRule selectAlertRuleById(Long id);

    /**
     * 查询报警规则列表
     * 
     * @param alertRule 报警规则
     * @return 报警规则集合
     */
    public List<AlertRule> selectAlertRuleList(AlertRule alertRule);

    /**
     * 新增报警规则
     * 
     * @param alertRule 报警规则
     * @return 结果
     */
    public int insertAlertRule(AlertRule alertRule);

    /**
     * 修改报警规则
     * 
     * @param alertRule 报警规则
     * @return 结果
     */
    public int updateAlertRule(AlertRule alertRule);

    /**
     * 删除报警规则
     * 
     * @param id 报警规则主键
     * @return 结果
     */
    public int deleteAlertRuleById(Long id);

    /**
     * 批量删除报警规则
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAlertRuleByIds(Long[] ids);

    /**
     * 查询所有启用的报警规则
     */
    @Select("SELECT * FROM alert_rule WHERE status = 1")
    List<AlertRule> selectEnabledRules();

    /**
     * 通过老人ID查询护理员用户ID列表
     * 随身设备：iotId → binding_location(elderId) → nursing_elder → nursing_id
     */
    @Select("SELECT nursing_id FROM nursing_elder WHERE elder_id = #{elderId}")
    List<Long> selectNursingIdByElderId(Long elderId);

    /**
     * 通过床位ID查询护理员用户ID列表
     * 床上设备：iotId → binding_location(bedId) → elder → nursing_elder → nursing_id
     */
    @Select("SELECT ne.nursing_id FROM nursing_elder ne " +
            "LEFT JOIN elder e ON ne.elder_id = e.id " +
            "LEFT JOIN bed b ON e.bed_id = b.id " +
            "WHERE b.id = #{bedId}")
    List<Long> selectNursingIdByBedId(Long bedId);
}
