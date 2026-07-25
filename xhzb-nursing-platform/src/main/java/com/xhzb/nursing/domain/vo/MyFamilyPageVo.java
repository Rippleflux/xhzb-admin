package com.xhzb.nursing.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyFamilyPageVo {
    private String mid;
    private String elderId;
    private String name;
    private String image;
    private String bedNumber;
    private String typeName;
    private String iotId;
    private String deviceName;
    private String productKey;
    private String remark;
}
