package com.xhzb.nursing.service.impl;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.exception.base.BaseException;
import com.xhzb.common.utils.SecurityUtils;
import com.xhzb.nursing.domain.HealthAssessmentDataCollection;
import com.xhzb.nursing.domain.HealthAssessmentReport;
import com.xhzb.nursing.domain.dto.health.ElderAssessmentDto;
import com.xhzb.nursing.domain.dto.health.HealthAssessmentDto;
import com.xhzb.nursing.domain.dto.health.MentalState;
import com.xhzb.nursing.mapper.HealthAssessmentDataCollectionMapper;
import com.xhzb.nursing.mapper.HealthAssessmentReportMapper;
import com.xhzb.nursing.service.IHealthAssessmentDataCollectionService;
import com.xhzb.nursing.service.IHealthAssessmentReportService;
import com.xhzb.oss.client.OSSAliyunFileStorageService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.HealthAssessmentMapper;
import com.xhzb.nursing.domain.HealthAssessment;
import com.xhzb.nursing.service.IHealthAssessmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 健康评估记录Service业务层处理
 *
 * @author rippleflux
 * @date 2026-07-13
 */
@Service
public class HealthAssessmentServiceImpl extends ServiceImpl<HealthAssessmentMapper, HealthAssessment> implements IHealthAssessmentService {
    @Autowired
    private HealthAssessmentMapper healthAssessmentMapper;

    @Autowired
    private IHealthAssessmentDataCollectionService healthAssessmentDataCollectionService;

    @Autowired
    HealthAssessmentDataCollectionMapper healthAssessmentDataCollectionMapper;

    @Autowired
    HealthAssessmentReportMapper healthAssessmentReportMapper;

    @Autowired
    private OSSAliyunFileStorageService fileStorageService;

    @Autowired
    @Qualifier("chatClientByAssessment")
    private ChatClient chatClient;

    @Autowired
    private IHealthAssessmentReportService healthAssessmentReportService;

    /**
     * 查询健康评估记录
     *
     * @param id 健康评估记录主键
     * @return 健康评估记录
     */
    @Override
    public HealthAssessmentDataCollection selectHealthAssessmentById(Long id) {
        return healthAssessmentDataCollectionMapper.selectById(id);
    }

    /**
     * 查询健康评估记录列表
     *
     * @param healthAssessment 健康评估记录
     * @return 健康评估记录
     */
    @Override
    public List<HealthAssessment> selectHealthAssessmentList(HealthAssessment healthAssessment) {
        return healthAssessmentMapper.selectHealthAssessmentList(healthAssessment);
    }

    /**
     * 新增健康评估记录
     *
     * @param data 健康评估记录
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long insertHealthAssessment(ElderAssessmentDto data) {
        return saveOrUpdateHealthAssessment(data);
    }

    private Long saveOrUpdateHealthAssessment(ElderAssessmentDto data) {

        HealthAssessment healthAssessment = new HealthAssessment();

        // 如果Id不为空, 并且从数据库中查询到健康评估记录数据, 在旧数据的基础上进行更新
        if (data.getId() != null) {
            healthAssessment = healthAssessmentMapper.selectById(data.getId());
            if (healthAssessment == null) {
                throw new ServiceException("健康评估不存在");
            }
            if (healthAssessment.getEvaluationProgress() != 0) {
                throw new ServiceException("健康评估已结束或取消");
            }
        }
        // 1、新增或修改健康评估记录表
        // 入住状态  默认为0  未入住
        healthAssessment.setCheckInStatus(0);
        // 核心建议  默认为null
        healthAssessment.setCoreSuggestion(null);
        // 评估进度  默认为0  评估中
        healthAssessment.setEvaluationProgress(0);
        // 老人姓名  从基本信息中获取
        healthAssessment.setElderName(data.getBasicInfo().getElderName());
        // 身份证号码  从基本信息中获取
        healthAssessment.setIdCard(data.getBasicInfo().getIdCard());
        // 保存，会自动主键返回
        saveOrUpdate(healthAssessment);

        // 2、新增或修改健康评估数据采集表数据
        HealthAssessmentDataCollection healthAssessmentDataCollection = new HealthAssessmentDataCollection();
        // 设置主键，与评估基本信息表的主键一致
        healthAssessmentDataCollection.setId(healthAssessment.getId());
        // 其余字段主要是为了方便展示使用，全部转换为JSON字符串中，存储到字段中
        // 基本信息
        healthAssessmentDataCollection.setBasicInfo(JSONUtil.toJsonStr(data.getBasicInfo()));
        // 健康评估
        healthAssessmentDataCollection.setHealthAssessment(JSONUtil.toJsonStr(data.getHealthAssessmentDto()));
        // 日常生活活动
        healthAssessmentDataCollection.setDailyLivingActivities(JSONUtil.toJsonStr(data.getDailyLivingActivities()));
        // 精神状态
        healthAssessmentDataCollection.setMentalState(JSONUtil.toJsonStr(data.getMentalState()));
        // 感知与沟通
        healthAssessmentDataCollection.setPerceptionCommunication(JSONUtil.toJsonStr(data.getPerceptionAndCommunication()));
        // 社会参与
        healthAssessmentDataCollection.setSocialParticipation(JSONUtil.toJsonStr(data.getSocialParticipation()));

        healthAssessmentDataCollectionService.saveOrUpdate(healthAssessmentDataCollection);

        //返回主键，方便前端查询或处理
        return healthAssessment.getId();
    }

    /**
     * 修改健康评估记录
     *
     * @param data 健康评估记录
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long updateHealthAssessment(ElderAssessmentDto data) {
        return saveOrUpdateHealthAssessment(data);
    }

    /**
     * 删除健康评估记录信息
     *
     * @param id 健康评估记录主键
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long deleteHealthAssessmentById(Long id) {
        healthAssessmentReportMapper.deleteHealthAssessmentReportByAssessmentId(id);
        healthAssessmentDataCollectionMapper.deleteHealthAssessmentDataCollectionById(id);
        healthAssessmentMapper.deleteHealthAssessmentById(id);
        return id;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long assessmentData(ElderAssessmentDto elderAssessmentDto) {
        // 获取体检报告链接
        String medicalReport = elderAssessmentDto.getHealthAssessmentDto().getRecent30Days().getMedicalReport();
        if (StrUtil.isEmpty(medicalReport)) {
            throw new ServiceException("请上传体检报告");
        }


        // 更新最新的健康评估数据
        Long id = saveOrUpdateHealthAssessment(elderAssessmentDto);

        // 获取评估相关需要的数据
        // 日常生活活动分级
        String dailyLiving = elderAssessmentDto.getDailyLivingActivities().getAbilityRating();
        // 精神状态分级
        String mentalState = elderAssessmentDto.getMentalState().getAbilityRating();
        // 感知觉与沟通分级
        String perceptionAndCommunication = elderAssessmentDto.getPerceptionAndCommunication().getAbilityRating();
        // 社会参与分级
        String socialParticipation = elderAssessmentDto.getSocialParticipation().getAbilityRating();
        // 最近30天健康数据
        HealthAssessmentDto.Recent30Days recent30Days = elderAssessmentDto.getHealthAssessmentDto().getRecent30Days();
        // 跌倒次数：0
        Integer fall = recent30Days.getFall();
        // 噎食次数：0
        Integer choking = recent30Days.getChoking();
        // 自杀次数:0
        Integer suicideAttempt = recent30Days.getSuicideAttempt();
        // 走失次数：0
        Integer lost = recent30Days.getLost();
        // 昏迷次数：0
        Integer coma = recent30Days.getComa();
        // 痴呆疾病：0
        HealthAssessmentDto.DiseaseDiagnosis diseaseDiagnosis = elderAssessmentDto.getHealthAssessmentDto().getDiseaseDiagnosis();
        String dementia = diseaseDiagnosis.getDementia();
        // 精神疾病：0
        String mentalIllness = diseaseDiagnosis.getMentalIllness();
        // 是否确诊为认知障碍
        MentalState.ClockDrawingTest clockDrawingTest = elderAssessmentDto.getMentalState().getClockDrawingTest();
        String clockDrawingResult = clockDrawingTest.getResult() == 2 ? "1" : clockDrawingTest.getResult().toString();
        // 拼接提示词
        String assessmentPrompt = getAssessmentPrompt(dailyLiving, mentalState, perceptionAndCommunication, socialParticipation, fall, choking, suicideAttempt, lost, coma, dementia, mentalIllness, clockDrawingResult);
        // 给AI大模型发生请求(获取评估结果)
        String content = chatClient.prompt().user(assessmentPrompt).call().content();
        // 解析评估结果
        content = content.replaceAll("```json", "").replaceAll("```", "");
        // 转换成JSON对象(本质是一个Map集合)
        JSONObject assessmentResult = JSONUtil.parseObj(content);
        String preLevel = assessmentResult.get("preLevel", String.class);
        String finalLevel = assessmentResult.get("finalLevel", String.class);
        String reason = assessmentResult.get("reason", String.class);

        // 老人体检报告评估
        // 1. 下载体检报告
        InputStream inputStream = fileStorageService.download(medicalReport);
        // 2. 读取体检报告文本
        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(new InputStreamResource(inputStream));
        List<Document> documentList = pdfReader.read();
        if (CollUtil.isEmpty(documentList)) {
            throw new BaseException("未读取到体检报告数据");
        }
        // 获取体检报告文本 List<Document>  --> List<String>{1,2,3,4} ---> 1234
        String medicalReportStr = documentList.stream().map(Document::getText).collect(Collectors.joining());
        // 拼接AI评估体检报告的提示词
        String medicalReportPrompt = getMedicalReportPrompt(medicalReportStr);
        // 调用大模型获取体检报告的评估结果
        String medicalReportContent = chatClient.prompt().user(medicalReportPrompt).call().content();
        if (StrUtil.isEmpty(medicalReportContent)) {
            throw new BaseException("AI大模型未返回体检报告评估数据");
        }
        medicalReportContent = medicalReportContent.replaceAll("```json", "").replaceAll("```", "");
        // 获取AI大模型返回的数据
        JSONObject medicalReportResult = JSONUtil.parseObj(medicalReportContent);


        // 健康评分
        Double healthScore = medicalReportResult.get("healthScore", Double.class);
        // 严重危险(健康, 提示, 风险, 危险, 严重危险)
        String riskLevel = medicalReportResult.get("riskLevel", String.class);
        // 异常分析
        String abnormalData = medicalReportResult.get("abnormalData", String.class);
        // 健康系统分值
        String systemScore = medicalReportResult.get("systemScore", String.class);
        // 报告总结
        String summarize = medicalReportResult.get("summarize", String.class);



        String medicalReportSuggestPrompt = getMedicalReportSuggestPrompt(
                summarize,
                abnormalData,
                dailyLiving,
                mentalState,
                perceptionAndCommunication,
                socialParticipation,
                reason);

        String medicalReportSuggestContent = chatClient.prompt().user(medicalReportSuggestPrompt).call().content();
        if (StrUtil.isEmpty(medicalReportSuggestContent)) {
            throw new BaseException("AI大模型未返回体检报告评估数据");
        }
        medicalReportSuggestContent = medicalReportSuggestContent.replaceAll("```json", "").replaceAll("```", "");
        // 获取AI大模型返回的数据
        JSONObject medicalReportSuggestResult = JSONUtil.parseObj(medicalReportSuggestContent);

        String coreSuggestion = medicalReportSuggestResult.get("core_suggestion", String.class);
        String suggestionDescription = medicalReportSuggestResult.get("suggestion_description", String.class);
        String recommendedCareLevel = medicalReportSuggestResult.get("recommended_care_level", String.class);
        String recommendedRoomType = medicalReportSuggestResult.get("recommended_room_type", String.class);
        String careFocus = medicalReportSuggestResult.get("care_focus", String.class);
        String dietSuggestion = medicalReportSuggestResult.get("diet_suggestion", String.class);
        String medicationNotes = medicalReportSuggestResult.get("medication_notes", String.class);
        String psychologicalCare = medicalReportSuggestResult.get("psychological_care", String.class);
        String familyCooperation = medicalReportSuggestResult.get("family_cooperation", String.class);
        String institutionPreparation = medicalReportSuggestResult.get("institution_preparation", String.class);

        // 将结果数据保存到评估报告表中
        HealthAssessmentReport assessmentReport = new HealthAssessmentReport();
        assessmentReport.setHealthAssessmentId(elderAssessmentDto.getId());
        assessmentReport.setAssessmentTime(LocalDateTime.now());
        assessmentReport.setAssessorName(SecurityUtils.getUsername());
        assessmentReport.setDailyActivityLevel(dailyLiving);
        assessmentReport.setMentalStatusLevel(mentalState);
        assessmentReport.setPerceptionCommunicationLevel(perceptionAndCommunication);
        assessmentReport.setSocialParticipationLevel(socialParticipation);
        assessmentReport.setInitialAbilityLevel(preLevel);
        assessmentReport.setFinalAbilityLevel(finalLevel);
        assessmentReport.setLevelChangeReason(reason);
        assessmentReport.setCheckInStatus(0);

        assessmentReport.setRemark(coreSuggestion);
        assessmentReport.setSuggestionDescription(suggestionDescription);
        assessmentReport.setRecommendedCareLevel(recommendedCareLevel);
        assessmentReport.setRecommendedRoomType(recommendedRoomType);
        assessmentReport.setCareFocus(careFocus);
        assessmentReport.setDietSuggestion(dietSuggestion);
        //assessmentReport.setMed(medicationNotes);
        assessmentReport.setPsychologicalCare(psychologicalCare);
        assessmentReport.setFamilyCooperation(familyCooperation);
        assessmentReport.setInstitutionPreparation(institutionPreparation);



        // 封装体检报告评估数据
        assessmentReport.setHealthScore(healthScore.toString());
        assessmentReport.setRiskLevel(riskLevel);
        assessmentReport.setAbnormalAnalysis(abnormalData);
        assessmentReport.setSystemScore(systemScore);
        assessmentReport.setReportSummary(summarize);
        // 是否建议入住
        assessmentReport.setCoreSuggestion(healthScore >= 60 ? 1 : 0);

        healthAssessmentReportMapper.insert(assessmentReport);




        // 更新健康评估表中的数据字段(core_suggestion,evaluation_progress)
        LambdaUpdateWrapper<HealthAssessment> updateWrapper = Wrappers.lambdaUpdate(HealthAssessment.class)
                .set(HealthAssessment::getCoreSuggestion, assessmentReport.getCoreSuggestion())
                .set(HealthAssessment::getEvaluationProgress, 1)
                .eq(HealthAssessment::getId, id);

        healthAssessmentMapper.update(updateWrapper);





        return id;
    }

    @Override
    public HealthAssessmentReport getAssessmentReportById(Long assessmentId) {
        // 构建条件根据健康评估Id查询报告详情
        LambdaQueryWrapper<HealthAssessmentReport> queryWrapper = Wrappers.lambdaQuery(HealthAssessmentReport.class)
                .eq(HealthAssessmentReport::getHealthAssessmentId, assessmentId);
        // 调用数据访问层查询单条数据(如果数据库中符合条件的有多条数据,报错)
        return healthAssessmentReportMapper.selectOne(queryWrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Long cancelHealthAssessment(Long assessmentId) {

        LambdaUpdateWrapper<HealthAssessment> updateWrapper = Wrappers.lambdaUpdate(HealthAssessment.class)
                .set(HealthAssessment::getEvaluationProgress, 2)
                .eq(HealthAssessment::getId, assessmentId);

        healthAssessmentMapper.update(updateWrapper);

        healthAssessmentReportMapper.deleteHealthAssessmentReportByAssessmentId(assessmentId);

        return assessmentId;
    }

    @Override
    public Map<String, Object> getElderInfoByAssessmentId(Long assessmentId) {
        HealthAssessment healthAssessment = healthAssessmentMapper.selectById(assessmentId);

        if (healthAssessment == null) {
            throw new ServiceException("健康评估不存在");
        }
        if (healthAssessment.getCheckInStatus() == 1) {
            throw new ServiceException("老人已入住,无需再次申请");
        }
        if (healthAssessment.getEvaluationProgress() != 1) {
            throw new ServiceException("健康评估未完成,请先完成健康评估");
        }

        LambdaQueryWrapper<HealthAssessmentDataCollection> queryWrapper = Wrappers.lambdaQuery(HealthAssessmentDataCollection.class)
                .select(HealthAssessmentDataCollection::getBasicInfo)
                .eq(HealthAssessmentDataCollection::getId, assessmentId);
        HealthAssessmentDataCollection healthAssessmentDataCollection = healthAssessmentDataCollectionMapper.selectOne(queryWrapper);


        if (healthAssessmentDataCollection == null || StrUtil.isEmpty(healthAssessmentDataCollection.getBasicInfo())) {
            throw new BaseException("健康评估数据不存在");
        }
        // 获取老人基本信息的JSON字符串
        String basicInfoJson = healthAssessmentDataCollection.getBasicInfo();
        // 转化为JSONObject对象
        JSONObject basicInfo = JSONUtil.parseObj(basicInfoJson);

        // 3. 封装结果返回
        Map<String, Object> result = new HashMap<>();
        result.put("coreSuggestion", healthAssessment.getCoreSuggestion());
        result.put("phone", basicInfo.getStr("elderContact"));
        result.put("medicalPaymentMethod", basicInfo.getStr("medicalPaymentMethod"));
        result.put("nation", basicInfo.getStr("nation"));
        result.put("educationLevel", basicInfo.getStr("educationLevel"));
        result.put("idCardNo", basicInfo.getStr("idCard"));
        result.put("name", basicInfo.getStr("elderName"));
        result.put("socialSecurityCard", basicInfo.getStr("socialSecurityCard"));
        result.put("livingSituation", basicInfo.getStr("livingSituation"));
        result.put("religiousBelief", basicInfo.getStr("religiousBelief"));
        result.put("economicSource", basicInfo.getStr("economicSource"));
        result.put("maritalStatus", basicInfo.getStr("maritalStatus"));

        return result;

    }

    private String getAssessmentPrompt(String dailyLiving, String mentalState, String perceptionAndCommunication, String socialParticipation, Integer fall, Integer choking, Integer suicideAttempt, Integer lost, Integer coma, String dementia, String mentalIllness, String cognitiveImpairment) {
        String promptTemplate = """
                ## 老人评估的的信息：
                - 日常生活活动分级：%s
                - 精神状态分级：%s
                - 感知觉与沟通分级：%s
                - 社会参与分级：%s
                - 跌倒次数：%s
                - 噎食次数：%s
                - 自杀次数：%s
                - 走失次数：%s
                - 昏迷次数：%s
                - 痴呆疾病：%s
                - 精神疾病：%s
                - 是否确诊为认知障碍：%s
                
                ## 评估规则1：
                - 能力完好：
                    日常生活活动、精神状态、感知觉与沟通分级均为0，社会参与分级为0或1
                - 轻度失能：
                    日常生活活动分级为0，但精神状态、感知觉与沟通中至少一项分级为1及以上，或社会参与的分级为2；
                    或日常生活活动分级为1，精神状态、感知觉与沟通、社会参与中至少有一项的分级为0或1
                - 中度失能：
                    日常生活活动分级为1，但精神状态、感知觉与沟通、社会参与均为2，或有一项为3；
                    或日常生活活动分级为2，且精神状态、感知觉与沟通、社会参与中有1-2项的分级为1或2
                - 重度失能：
                    日常生活活动的分级为3；
                    或日常生活活动、精神状态、感知觉与沟通、社会参与分级均为2；
                    或日常生活活动分级为2，且精神状态、感知觉与沟通、社会参与中至少有一项分级为3
                
                ## 评估原则2：
                1.有认知障碍/痴呆、精神疾病者，在原有能力级别上提高一个等级；
                2.近30天内发生过2次及以上跌倒、噎食、自杀、走失者，在原有能力级别上提高一个等级；
                3.处于昏迷状态者，直接评定为重度失能；
                4.若初步等级确定为“3重度失能”，则不考虑上述1-3中各情况对最终等级的影响，等级不再提高
                
                ## 匹配规则
                1. 请根据老人的评估信息与规则1逐条进行比对，判断老人属于哪一种能力
                2. 然后拿老人的评估信息逐条与规则2进行比对，再次判断老人属于哪一种能力。
                3. 如果两次评级不一样，升级的理由是什么，理由只需要填写评估原则2 的一条或多条内容，把内容输出到reason中，不需要说明分析理由
                4. 结合评估的原则，给出老人的两次评级，不需要输出分析过程，只需要输出json格式，不要出现markdown语法
                格式为：
                {{
                    "preLevel": "能力完好|轻度失能|中度失能|重度失能",
                    "finalLevel": "能力完好|轻度失能|中度失能|重度失能",
                    "reason":"评估原则2中一条或多条"
                }}
                """;

        return String.format(promptTemplate, dailyLiving, mentalState, perceptionAndCommunication, socialParticipation, fall.toString(), choking.toString(), suicideAttempt.toString(), lost.toString(), coma.toString(), dementia, mentalIllness, cognitiveImpairment);
    }

    private String getMedicalReportPrompt(String medicalReportStr) {
        String template = """
                请以一个专业医生的视角来分析这份体检报告，报告中包含了一些异常数据，我需要您对这些数据进行解读，并给出相应的健康建议。
                体检内容如下：
                %s
                
                要求：
                1. 提取体检报告中的“总检日期”；
                2. 通过临床医学、疾病风险评估模型和数据智能分析，给该用户的风险等级和健康指数给出结果。风险等级分为：健康、提示、风险、危险、严重危险。健康指数范围为0至100分；
                3. 对于体检报告有异常数据，请列出（异常数据的结论、体检项目名称、检查结果、参考值、单位、异常解读、建议）这8字段。解读异常数据，解决这些数据可能代表的健康问题或风险。分析可能的原因，包括但不限于生活习惯、饮食习惯、遗传因素等。基于这些异常数据和可能的原因，请给出具体的健康建议，包括饮食调整、运动建议、生活方式改变以及是否需要进一步检查或治疗等。
                结论格式：异常数据的结论：肥胖，体检项目名称：体重指数BMI，检查结果：29.2，参考值>24，单位：-。异常解读：体重超标包括超重与肥胖。体重指数（BMI）=体重（kg）/身⾼（m）的平⽅，BMI≥24为超重，BMI≥28为肥胖；男性腰围≥90cm和⼥性腰围≥85cm为腹型肥胖。体重超标是⼀种由多因素（如遗传、进⻝油脂较多、运动少、疾病等）引起的慢性代谢性疾病，尤其是肥胖，已经被世界卫⽣组织列为导致疾病负担的⼗⼤危险因素之⼀。AI建议：采取综合措施预防和控制体重，积极改变⽣活⽅式，宜低脂、低糖、⾼纤维素膳⻝，多⻝果蔬及菌藻类⻝物，增加有氧运动。若有相关疾病（如⾎脂异常、⾼⾎压、糖尿病等）应积极治疗。
                4. 根据这个体检报告的内容，分别是给人体的8大系统打分，每项满分为100分，8大系统分别为：呼吸系统、消化系统、内分泌系统、免疫系统、循环系统、泌尿系统、运动系统、感官系统
                5. 给体检报告做一个总结，总结格式：体检报告中尿蛋⽩、癌胚抗原、⾎沉、空腹⾎糖、总胆固醇、⽢油三酯、低密度脂蛋⽩胆固醇、⾎清载脂蛋⽩B、动脉硬化指数、⽩细胞、平均红细胞体积、平均⾎红蛋⽩共12项指标提示异常，尿液常规共1项指标处于临界值，⾎脂、⾎液常规、尿液常规、糖类抗原、⾎清酶类等共43项指标提示正常，综合这些临床指标和数据分析：肾脏、肝胆、⼼脑⾎管存在隐患，其中⼼脑⾎管有“⾼危”⻛险；肾脏部位有“中危”⻛险；肝胆部位有“低危”⻛险。
                
                # 输出要求：
                最后，将以上结果输出为纯JSON格式，不要包含其他的文字说明，也不要出现Markdown语法相关的文字，所有的返回结果都是json，注意双引号的单引号的配合使用，详细格式如下：
                
                {
                  "healthScore": XX.XX,
                  "riskLevel": "健康|提示|风险|危险|严重危险",
                  "abnormalData": [
                    {
                      "conclusion": "异常数据的结论",
                      "examinationItem": "体检项目名称",
                      "result": "检查结果",
                      "referenceValue": "参考值",
                      "unit": "单位",
                      "interpret":"对于异常的结论进一步详细的说明",
                      "advice":"针对于这一项的异常，给出一些健康的建议"
                    }
                  ],
                  "systemScore": {
                    "breathingSystem": XX,
                    "digestiveSystem": XX,
                    "endocrineSystem": XX,
                    "immuneSystem": XX,
                    "circulatorySystem": XX,
                    "urinarySystem": XX,
                    "motionSystem": XX,
                    "senseSystem": XX
                  },
                  "summarize": "体检报告的总结"
                }
                """;
        return String.format(template, medicalReportStr);
    }


    private String getMedicalReportSuggestPrompt(String report_summary, String abnormal_analysis, String daily_activity_level, String mental_status_level, String perception_communication_level, String social_participation_level, String remark) {
        String template = """
                # 角色
                   你是一位顶尖的老年综合评估专家和长期照护顾问。你能够深度整合老年人的健康体检报告、能力评估、疾病诊断及社会心理等多维度信息，为养老机构提供精准、专业、可执行的照护建议。
                
                   # 任务
                   请严格依据提供的《老年人能力评估报告》数据，生成一份全面的照护指导。你必须遵循以下核心推导逻辑，并以指定的JSON格式输出最终结果。
                
                   # 核心推导逻辑与字段映射
                
                   1.  **核心建议 (`core_suggestion`)**：这是对“老年人能力最终等级 + 核心健康风险”的战略性浓缩。它直接决定了整体照护方向。例如，若最终等级为重度失能，核心建议应为“需提供全面重度护理服务，重点防范因重度痴呆导致的走失和跌倒风险”。
                
                   2.  **建议说明 (`suggestion_description`)**：这是对`core_suggestion`的详细论证。你必须清晰阐述推导过程，将“关键指标得分”与“核心建议”直接关联。内容必须包含：
                       *   引用四个一级指标（日常生活活动、精神状态、感知觉与沟通、社会参与）的具体得分和关键分项表现。
                       *   引用疾病诊断详情（如痴呆类型与程度）、意外事件发生情况。
                       *   清晰说明等级变更条款的应用逻辑（例如：“因存在中度痴呆，在初步等级基础上提高一个等级，故最终建议为重度护理”）。
                
                   3.  **推荐护理等级 (`recommended_care_level`)**：直接映射“老年人能力最终等级”。
                       *   0级 → 无需特殊护理
                       *   1级 → 轻度护理
                       *   2级 → 中度护理
                       *   3级 → 重度护理
                       *   *特殊修正*：若体检报告显示老人有需要频繁医疗干预的慢性病（如晚期帕金森、需长期输液），可在能力等级基础上酌情提升护理等级，并在此处说明。
                
                   4.  **推荐入住房型 (`recommended_room_type`)**：基于“能力短板”和“安全风险”推荐房型。你必须综合考量：
                       *   **护理便捷性**：根据护理等级判断是否需要靠近护理站。
                       *   **安全防护**：
                           *   有跌倒史(≥2次)或行走能力差 → 防滑、无障碍房型。
                           *   有走失风险或痴呆 → 防走失房型（带门禁、定位设施）。
                           *   有攻击行为 → 单人隔离房型。
                       *   **特殊功能需求**：根据视觉/听觉障碍、是否需要医疗设备等，推荐带呼叫系统、医疗接口或特殊照明的房型。
                       *   *说明示例*：“推荐入住靠近护理站的防走失无障碍双人间，房间配备护理床、防滑地面、卫生间扶手及24小时呼叫系统。”
                
                   5.  **护理重点 (`care_focus`)**：聚焦“能力短板 + 高风险事件”，列出最优先的3-5项护理任务。例如：“1. 进食协助与防噎食护理；2. 认知功能训练与定向力维护；3. 防跌倒/坠床的24小时监护；4. 皮肤护理与压疮预防；5. 情绪安抚及异常行为监测。”
                
                   6.  **饮食建议 (`diet_suggestion`)**：兼顾“进食能力 + 疾病禁忌 + 营养需求”。
                       *   根据`日常生活活动`中的进食得分，判断食物质地（如鼻饲流食、软食、易嚼碎食）。
                       *   根据疾病诊断和体检异常指标（血糖、血压、血脂、肾功能），给出治疗性饮食原则（如低盐低脂糖尿病饮食）。
                       *   有噎食史，必须强调防噎食饮食形态（小块、无粘性、避免干硬食物）。
                
                   7.  **用药注意事项 (`medication_notes`)**：核心是“疾病适配 + 用药安全”。
                       *   结合老人正在使用的药物清单及疾病（如高血压、糖尿病、痴呆），提出用药监测要点。
                       *   根据体检报告中的肝肾功能等指标，提示是否有药物代谢风险，需要调整剂量或监测不良反应。
                       *   结合老人的认知和精神状态，判断是否需要“送药到手、看服到口”的专人监护用药服务。
                
                   8.  **心理关怀建议 (`psychological_care`)**：聚焦“情绪状态 + 社交能力 + 认知水平”。
                       *   根据`精神状态`中的抑郁、焦虑、攻击行为等得分，给出具体的情绪疏导、兴趣激发或安抚技巧。
                       *   根据`社会参与`中的社会交往和人物定向能力，提供社交引导、现实定向或怀旧疗法等建议。
                       *   关注婚姻、居住情况等社会背景，提供情感陪伴建议。
                
                   9.  **家属配合事项 (`family_cooperation`)**：围绕“机构照护延伸 + 家庭支持”。明确家属需配合的具体行动，如：
                       *   为痴呆老人提供过往熟悉照片，协助记忆唤醒。
                       *   提供防滑鞋，与机构共同记录和分析意外事件原因。
                       *   定期探望以稳定老人情绪，并配合机构的情绪安抚技巧。
                       *   监督居家时的饮食和用药，保证照护的连续性。
                
                   10. **机构准备事项 (`institution_preparation`)**：聚焦“硬件适配 + 人力配置 + 安全防护”。
                       *   根据护理等级和功能障碍，准备辅助器具（护理床、轮椅、助行器、导尿管护理用品等）。
                       *   根据感官障碍，准备辅助设备（放大镜、助听器、沟通卡片）。
                       *   根据安全风险，准备防护用品（防滑垫、床栏、防走失手环、移除房间内尖锐物品等），并配置相应的监护人力。
                
                   # 输入数据
                   请严格基于以下《老年人能力评估报告》数据进行思考和分析：
                   **1. 健康体检报告关键指标与疾病诊断：**
                   {health_report_data}
                   %s
                   \\
                   %s
                
                   **2. 日常生活活动能力评估 (A.4)：**
                   {adl_assessment_data}
                   %s
                
                   **3. 精神状态评估 (A.5)：**
                   {mental_state_assessment_data}
                   %s
                
                   **4. 感知觉与沟通能力评估 (A.6)：**
                   {sensory_communication_assessment_data}
                   %s
                
                   **5. 社会参与能力评估 (A.7)：**
                   {social_participation_assessment_data}
                   %s
                
                   **6. 其他关键信息（如意外事件史、用药情况等）：**
                   {other_key_info}
                   %s
                
                   # 输出要求
                   1.  输出必须是一个合法的JSON对象，不要包含任何markdown代码块标记（如```json）或任何其他解释性文字。
                   2.  JSON对象中的所有字段都必须以双引号包裹。
                   3.  每个字段的值必须是一个字符串，其中可以包含用于格式化的换行符`\\n`，以便于阅读。所有建议需分点描述，并用数字序号列出。
                   4.  请保证输出的完整性和专业性，所有建议都必须直接来源于对输入数据的分析。
                   5.  如果某项输入数据缺失或未提供，请根据其他已有信息进行合理推断并输出，不要留空或输出`null`。
                
                   # 输出JSON格式示例
                   {
                     "core_suggestion": "基于能力最终等级和核心风险的战略性建议。",
                     "suggestion_description": "对核心建议的详细推导说明，关联具体得分。\\n1. 能力等级推导：...\\n2. 关键健康风险：...",
                     "recommended_care_level": "重度护理 (3级)",
                     "recommended_room_type": "靠近护理站的防走失单人间。\\n1. 房间配置：护理床、防滑地面、床栏。\\n2. 安全设施：门禁系统、24小时呼叫铃、定位手环。\\n3. 推荐理由：基于重度失能和走失风险。",
                     "care_focus": "1. 24小时全面生活护理（进食、洗漱、如厕）。\\n2. 重点防范走失与跌倒风险。\\n3. 认知功能维持与情绪安抚。\\n4. 慢性病指标监测。",
                     "diet_suggestion": "低盐低脂糖尿病软食。\\n1. 食物质地：软烂易咀嚼，小块进食。\\n2. 营养原则：控制总热量，定时定量。\\n3. 特别禁忌：避免粘性食物，防范噎食风险。",
                     "medication_notes": "1. 降压/降糖药须严格遵医嘱，服药后监测血压血糖。\\n2. 老人认知功能下降，须专人看护服药，确保“看服到口”。\\n3. 因肾功能指标异常（肌酐XXX），需警惕药物蓄积，遵医嘱调整剂量。",
                     "psychological_care": "1. 实施怀旧疗法，用熟悉物品和环境缓解焦虑。\\n2. 每日进行简单社交互动，避免其孤独感加重。\\n3. 密切关注情绪波动，烦躁时进行耐心安抚，避免刺激。",
                     "family_cooperation": "1. 提供老人年轻时的照片和喜爱物件，协助记忆唤醒。\\n2. 建议固定时间探望，增强其安全感。\\n3. 向机构反馈老人居家时的行为习惯和情绪触发点。",
                     "institution_preparation": "1. 准备护理床、防压疮气垫床。\\n2. 房间内安装防滑扶手，移除尖锐危险品。\\n3. 配置防走失定位手环及门禁。\\n4. 准备专用喂食餐具，并培训护理员海姆立克急救法。\\n5. 配置24小时专人监护人力。"
                   }
                """;
        return String.format(template, report_summary, abnormal_analysis, daily_activity_level, mental_status_level, perception_communication_level, social_participation_level, remark);
    }
}
