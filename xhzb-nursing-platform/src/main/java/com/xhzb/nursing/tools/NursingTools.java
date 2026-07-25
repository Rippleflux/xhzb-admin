package com.xhzb.nursing.tools;

import com.xhzb.nursing.domain.NursingLevel;
import com.xhzb.nursing.domain.NursingPlan;
import com.xhzb.nursing.domain.NursingProject;
import com.xhzb.nursing.domain.vo.NursingLevelVo;
import com.xhzb.nursing.domain.vo.NursingProjectPlanVo;
import com.xhzb.nursing.mapper.NursingProjectPlanMapper;
import com.xhzb.nursing.service.INursingLevelService;
import com.xhzb.nursing.service.INursingPlanService;
import com.xhzb.nursing.service.INursingProjectService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 养老院护理相关工具类（Tool Calling）
 */
@Component
public class NursingTools {

    @Autowired
    private INursingPlanService nursingPlanService;

    @Autowired
    private INursingLevelService nursingLevelService;

    @Autowired
    private INursingProjectService nursingProjectService;

    @Autowired
    private NursingProjectPlanMapper nursingProjectPlanMapper;

    /**
     * 查询所有护理计划及其包含的护理项目
     */
    @Tool(name = "queryAllNursingPlans", description = "查询养老院所有护理计划，每个计划包含其关联的护理项目列表（项目名称、价格、单位、护理要求等）")
    public List<Map<String, Object>> queryAllNursingPlans() {
        // 查询所有启用的护理计划
        List<NursingPlan> plans = nursingPlanService.listAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (NursingPlan plan : plans) {
            Map<String, Object> planMap = new HashMap<>();
            planMap.put("id", plan.getId());
            planMap.put("planName", plan.getPlanName());

            // 查询该计划关联的护理项目
            List<NursingProjectPlanVo> projectPlans = nursingProjectPlanMapper.selectByPlanId(plan.getId());
            List<Map<String, Object>> projects = new ArrayList<>();

            for (NursingProjectPlanVo pp : projectPlans) {
                NursingProject project = nursingProjectService.selectNursingProjectById(Long.parseLong(pp.getProjectId()));
                if (project != null) {
                    Map<String, Object> projectMap = new HashMap<>();
                    projectMap.put("projectName", project.getName());
                    projectMap.put("price", project.getPrice());
                    projectMap.put("unit", project.getUnit());
                    projectMap.put("nursingRequirement", project.getNursingRequirement());
                    projectMap.put("executeTime", pp.getExecuteTime());
                    projectMap.put("executeCycle", formatCycle(pp.getExecuteCycle()));
                    projectMap.put("executeFrequency", pp.getExecuteFrequency());
                    projects.add(projectMap);
                }
            }

            planMap.put("projects", projects);
            result.add(planMap);
        }

        return result;
    }

    /**
     * 查询所有护理等级（含价格及关联的护理计划和护理项）
     */
    @Tool(name = "queryAllNursingLevels", description = "查询养老院所有护理等级，包含等级名称、护理费用、关联的护理计划名称，以及该计划下的护理项目列表")
    public List<Map<String, Object>> queryAllNursingLevels() {
        // 查询所有护理等级（含关联的护理计划名称）
        NursingLevel query = new NursingLevel();
        List<NursingLevelVo> levels = nursingLevelService.selectNursingLevelList(query);
        List<Map<String, Object>> result = new ArrayList<>();

        for (NursingLevelVo level : levels) {
            Map<String, Object> levelMap = new HashMap<>();
            levelMap.put("id", level.getId());
            levelMap.put("levelName", level.getName());
            levelMap.put("fee", level.getFee());
            levelMap.put("description", level.getDescription());
            levelMap.put("planName", level.getPlanName());

            // 查询该等级关联的护理计划下的护理项目
            if (level.getLplanId() != null) {
                List<NursingProjectPlanVo> projectPlans = nursingProjectPlanMapper.selectByPlanId(level.getLplanId());
                List<String> projectNames = new ArrayList<>();

                for (NursingProjectPlanVo pp : projectPlans) {
                    NursingProject project = nursingProjectService.selectNursingProjectById(Long.parseLong(pp.getProjectId()));
                    if (project != null) {
                        projectNames.add(project.getName());
                    }
                }

                levelMap.put("nursingProjects", projectNames);
            }

            result.add(levelMap);
        }

        return result;
    }

    /**
     * 格式化执行周期
     */
    private String formatCycle(Integer cycle) {
        if (cycle == null) {
            return "未知";
        }
        return switch (cycle) {
            case 0 -> "每天";
            case 1 -> "每周";
            case 2 -> "每月";
            default -> "未知";
        };
    }
}
