package com.xhzb.nursing.domain.vo.checkIn;

import com.xhzb.nursing.domain.Contract;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 入住详情响应模型
 *
 * @author itcast
 * @create 2023/12/19 14:58
 **/
@Schema(description = "入住详情响应模型")
@Data
public class CheckInDetailVo {
    /**
     * 老人信息
     */
    @Schema(title = "老人响应信息")
    private CheckInElderVo checkInElderVo;

    /**
     * 家属信息
     */
    @Schema(title = "家属响应信息")
    private List<ElderFamilyVo> elderFamilyVoList;

    /**
     * 入住配置
     */
    @Schema(title = "入住配置响应信息")
    private CheckInConfigVo checkInConfigVo;

    /**
     * 签约办理
     */
    @Schema(title = "签约办理响应信息")
    private Contract contract;

}
