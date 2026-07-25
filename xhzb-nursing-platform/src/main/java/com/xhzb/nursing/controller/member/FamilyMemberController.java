package com.xhzb.nursing.controller.member;

import com.xhzb.common.annotation.Log;
import com.xhzb.common.core.domain.R;
import com.xhzb.common.core.page.TableDataInfo;
import com.xhzb.common.enums.BusinessType;
import com.xhzb.nursing.domain.FamilyMemberElder;
import com.xhzb.nursing.domain.NursingProject;
import com.xhzb.nursing.domain.Reservation;
import com.xhzb.nursing.domain.dto.AddFamilyDto;
import com.xhzb.nursing.domain.dto.DeviceDetailVo;
import com.xhzb.nursing.domain.dto.UserLoginRequestDto;
import com.xhzb.nursing.domain.vo.LoginVo;
import com.xhzb.nursing.domain.vo.MyFamilyPageVo;
import com.xhzb.nursing.domain.vo.MyFamilyVo;
import com.xhzb.nursing.service.*;
import com.xhzb.nursing.service.impl.DeviceServiceImpl;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.xhzb.common.core.controller.BaseController;
import com.xhzb.common.core.domain.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

/**
 * 老人家属Controller
 *
 * @author ruoyi
 * @date 2024-09-02
 */
@RestController
@RequestMapping("/member")
@Tag(name = "老人家属相关接口")
public class FamilyMemberController extends BaseController {
    @Autowired
    private IFamilyMemberService familyMemberService;

    @Autowired
    private INursingProjectService nursingProjectService;

    @Autowired
    private IFamilyMemberElderService familyMemberElderService;

    @Autowired
    private IDeviceService deviceService;


    @PostMapping("/user/login")
    @Operation(summary = "小程序登录")
    public AjaxResult login(@RequestBody UserLoginRequestDto userLoginRequestDto) {
        LoginVo loginVo = familyMemberService.login(userLoginRequestDto);
        return success(loginVo);
    }

    @GetMapping("/orders/project/page")
    @Operation(summary = "分页查询护理项目列表")
    public TableDataInfo getNursingProjectList(NursingProject nursingProject) {
        startPage();
        List<NursingProject> list = nursingProjectService.selectNursingProjectList(nursingProject);
        return getDataTable(list);
    }

    @GetMapping("/orders/project/{id}")
    @Operation(summary = "根据编号查询护理项目信息")
    public R<NursingProject> getNursingProjectInfo(@PathVariable Long id) {
        NursingProject nursingProject = nursingProjectService.selectNursingProjectById(id);
        return R.ok(nursingProject);
    }


    @PostMapping("/user/add")
    @Operation(summary = "新增绑定家人")
    public AjaxResult addFamily(@RequestBody AddFamilyDto data) {
        familyMemberService.addFamily(data);
        return success();
    }


    @GetMapping("/user/my")
    @Operation(summary = "查询当前绑定家属列表")
    public AjaxResult getMyFamily() {
        List<MyFamilyVo> list = familyMemberService.getMyFamily();
        return success(list);
    }

    @GetMapping("/user/list-by-page")
    @Operation(summary = "1.3 查看家人列表（分页查询）")
    public TableDataInfo getMyFamilyList(FamilyMemberElder familyMemberElder) {
        startPage();
        List<MyFamilyPageVo> list = familyMemberElderService.getMyFamilyList();
        return getDataTable(list);
    }

    @GetMapping("/user/queryDeviceDetail/{iotId}")
    @Operation(summary = "1.4 查询健康数据")
    public AjaxResult queryServiceProperties(@PathVariable String iotId) {
        DeviceDetailVo deviceDetailVo = deviceService.queryDeviceDetail(iotId);
        return success(deviceDetailVo);
    }
}