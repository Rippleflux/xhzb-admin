package com.xhzb.nursing.service.impl;

import java.util.List;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xhzb.common.exception.ServiceException;
import com.xhzb.common.utils.UserThreadLocal;
import com.xhzb.framework.web.service.TokenService;
import com.xhzb.nursing.domain.Elder;
import com.xhzb.nursing.domain.FamilyMemberElder;
import com.xhzb.nursing.domain.dto.AddFamilyDto;
import com.xhzb.nursing.domain.dto.UserLoginRequestDto;
import com.xhzb.nursing.domain.vo.LoginVo;
import com.xhzb.nursing.domain.vo.MyFamilyPageVo;
import com.xhzb.nursing.domain.vo.MyFamilyVo;
import com.xhzb.nursing.mapper.ElderMapper;
import com.xhzb.nursing.mapper.FamilyMemberElderMapper;
import com.xhzb.nursing.service.WechatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.FamilyMemberMapper;
import com.xhzb.nursing.domain.FamilyMember;
import com.xhzb.nursing.service.IFamilyMemberService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 老人家属Service业务层处理
 *
 * @author ruoyi
 * @date 2026-07-17
 */
@Slf4j
@Service
public class FamilyMemberServiceImpl extends ServiceImpl<FamilyMemberMapper, FamilyMember> implements IFamilyMemberService {
    @Autowired
    private FamilyMemberMapper familyMemberMapper;

    @Autowired
    WechatService wechatService;

    @Autowired
    ElderMapper elderMapper;

    @Autowired
    FamilyMemberElderMapper familyMemberElderMapper;

    static List<String> DEFAULT_NICKNAME_PREFIX = ListUtil.of("生活更美好",
            "大桔大利",
            "日富一日",
            "好柿开花",
            "柿柿如意",
            "一椰暴富",
            "大柚所为",
            "杨梅吐气",
            "天生荔枝"
    );
    @Autowired
    private TokenService tokenService;

    /**
     * 查询老人家属
     *
     * @param id 老人家属主键
     * @return 老人家属
     */
    @Override
    public FamilyMember selectFamilyMemberById(Long id) {
        return getById(id);
    }

    /**
     * 查询老人家属列表
     *
     * @param familyMember 老人家属
     * @return 老人家属
     */
    @Override
    public List<FamilyMember> selectFamilyMemberList(FamilyMember familyMember) {
        return familyMemberMapper.selectFamilyMemberList(familyMember);
    }

    /**
     * 新增老人家属
     *
     * @param familyMember 老人家属
     * @return 结果
     */
    @Override
    public int insertFamilyMember(FamilyMember familyMember) {
        return save(familyMember) ? 1 : 0;
    }

    /**
     * 修改老人家属
     *
     * @param familyMember 老人家属
     * @return 结果
     */
    @Override
    public int updateFamilyMember(FamilyMember familyMember) {
        return updateById(familyMember) ? 1 : 0;
    }

    /**
     * 批量删除老人家属
     *
     * @param ids 需要删除的老人家属主键
     * @return 结果
     */
    @Override
    public int deleteFamilyMemberByIds(Long[] ids) {
        return removeByIds(Arrays.asList(ids)) ? 1 : 0;
    }

    /**
     * 删除老人家属信息
     *
     * @param id 老人家属主键
     * @return 结果
     */
    @Override
    public int deleteFamilyMemberById(Long id) {
        return removeById(id) ? 1 : 0;
    }

    @Override
    public LoginVo login(UserLoginRequestDto userLoginRequestDto) {

        // 1-登录凭证校验接口调用：openId
        String openid = wechatService.getOpenid(userLoginRequestDto.getCode());

        String phoneNumber = wechatService.getPhone(userLoginRequestDto.getPhoneCode());
        // 2-根据openId查询家属表进行判断
        LambdaQueryWrapper<FamilyMember> queryWrapper = Wrappers.lambdaQuery(FamilyMember.class).eq(FamilyMember::getOpenId, openid);
        FamilyMember familyMember = familyMemberMapper.selectOne(queryWrapper);
        int index = RandomUtil.randomInt(0, 9);
        String name = DEFAULT_NICKNAME_PREFIX.get(index) + phoneNumber.substring(phoneNumber.length() - 4); // 2.1-如果是新用户：新增


        if (ObjectUtils.isEmpty(familyMember)) {
            familyMember = FamilyMember.builder().phone(phoneNumber)
                    .openId(openid)
                    .name(name)
                    .build();
            familyMemberMapper.insert(familyMember);
        }

        // 2.1-如果是老用户：更新
        if (!familyMember.getPhone().equals(phoneNumber)) {
            familyMember.setPhone(phoneNumber);
            familyMember.setName(name);
            familyMemberMapper.updateFamilyMember(familyMember);
        }
        // 3-基于用户id构建token：jwt的token
        Map<String, Object> claims = MapUtil.<String, Object>builder().put("userId", familyMember.getId()).build();
        String token = tokenService.createToken(claims);
        return LoginVo.builder().token(token).nickName(familyMember.getName()).build();
    }

    @Override
    public void addFamily(AddFamilyDto data) {
        String idCard = data.getIdCard();
        String name = data.getName();
        LambdaQueryWrapper<Elder> queryWrapper = Wrappers.lambdaQuery(Elder.class)
                .eq(StrUtil.isNotBlank(idCard), Elder::getIdCardNo, idCard)
                .eq(StrUtil.isNotBlank(name), Elder::getName, name)
                .in(Elder::getStatus, 1, 2);
        Elder elder = elderMapper.selectOne(queryWrapper);

        if (ObjectUtils.isNull(elder)) {
            throw new ServiceException("当前老人未入住养老院");
        }

        Long userId = UserThreadLocal.getUserId();

        FamilyMemberElder familyMemberElder = FamilyMemberElder.builder().familyMemberId(userId).elderId(elder.getId()).build();
        familyMemberElder.setRemark(data.getRemark());
        familyMemberElderMapper.insert(familyMemberElder);

    }

    @Override
    public List<MyFamilyVo> getMyFamily() {
        Long userId = UserThreadLocal.getUserId();
        if (ObjectUtils.isNull(userId)) {
            throw new ServiceException("查询失败");
        }
        LambdaQueryWrapper<FamilyMemberElder> queryWrapper = Wrappers.lambdaQuery(FamilyMemberElder.class).eq(ObjectUtils.isNotNull(userId), FamilyMemberElder::getFamilyMemberId, userId);
        List<FamilyMemberElder> familyMemberElders = familyMemberElderMapper.selectList(queryWrapper);

        List<Long> list = familyMemberElders.stream().map(FamilyMemberElder::getElderId).toList();

        List<Elder> elders = elderMapper.selectList(Wrappers.lambdaQuery(Elder.class).in(Elder::getId, list));

        Map<Long, String> collect = elders.stream().collect(Collectors.toMap(Elder::getId, Elder::getName));

        return familyMemberElders.stream().map(item ->
                MyFamilyVo.builder()
                        .id(item.getId())
                        .elderId(item.getElderId())
                        .familyMemberId(item.getFamilyMemberId())
                        .elderName(collect.getOrDefault(item.getElderId(), ""))
                        .build()
        ).collect(Collectors.toList());

    }


}
