package com.xhzb.nursing.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyFamilyVo {
    private Long id;
    private Long familyMemberId;
    private Long elderId;
    private String elderName;
}
