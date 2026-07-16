package com.xhzb.nursing.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xhzb.nursing.domain.AiConversation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * AI会话历史Mapper接口
 */
@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {

    /**
     * 根据用户ID查询会话列表
     *
     * @param createBy 创建人ID
     * @return 会话列表
     */
    List<AiConversation> selectConversationListByUserId(Long createBy);
}
