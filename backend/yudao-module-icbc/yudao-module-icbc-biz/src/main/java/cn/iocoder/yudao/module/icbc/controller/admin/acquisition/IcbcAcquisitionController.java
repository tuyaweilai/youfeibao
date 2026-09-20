package cn.iocoder.yudao.module.icbc.controller.admin.acquisition;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.acquisition.vo.*;
import cn.iocoder.yudao.module.icbc.dal.dataobject.acquisition.IcbcAcquisitionDO;
import cn.iocoder.yudao.module.icbc.enums.AcquisitionStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.acquisition.AcquisitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 收购登记。
 *
 * <p>对应 issue #7：收货员在现场登记一笔收购，登记完这一笔的合同流、货物流、信息流骨架
 * 即成形；可导出单笔收购确认书（可打印），并能看到该笔卡在哪一步。
 */
@Tag(name = "管理后台 - 收购登记")
@RestController
@RequestMapping("/icbc/acquisition")
@Validated
public class IcbcAcquisitionController {

    @Resource
    private AcquisitionService acquisitionService;

    @PostMapping("/create")
    @Operation(summary = "登记一笔收购")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.ACQUISITION_CREATE + "')")
    public CommonResult<AcquisitionCreateRespVO> createAcquisition(@Valid @RequestBody AcquisitionCreateReqVO reqVO) {
        return success(acquisitionService.createAcquisition(reqVO));
    }

    @PostMapping("/sync-offline")
    @Operation(summary = "离线补传：批量登记，重复补传不产生重复单据")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.ACQUISITION_CREATE + "')")
    public CommonResult<List<AcquisitionSyncResultVO>> syncOffline(
            @Valid @RequestBody AcquisitionOfflineSyncReqVO reqVO) {
        return success(acquisitionService.syncOffline(reqVO));
    }

    @PostMapping("/correct")
    @Operation(summary = "人工修正磅单 / 车牌的识别结果")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.ACQUISITION_UPDATE + "')")
    public CommonResult<Boolean> correctRecognition(@Valid @RequestBody AcquisitionCorrectionReqVO reqVO) {
        acquisitionService.correctRecognition(reqVO);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得收购单")
    @Parameter(name = "id", description = "收购单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.ACQUISITION_QUERY + "')")
    public CommonResult<AcquisitionRespVO> getAcquisition(@RequestParam("id") Long id) {
        return success(toRespVO(acquisitionService.getAcquisition(id)));
    }

    @GetMapping("/page")
    @Operation(summary = "分页获得收购单")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.ACQUISITION_QUERY + "')")
    public CommonResult<PageResult<AcquisitionRespVO>> getAcquisitionPage(
            @Valid AcquisitionPageReqVO pageReqVO) {
        PageResult<IcbcAcquisitionDO> page = acquisitionService.getAcquisitionPage(pageReqVO);
        return success(new PageResult<>(page.getList().stream()
                .map(this::toRespVO).collect(Collectors.toList()), page.getTotal()));
    }

    @GetMapping("/list-by-payee")
    @Operation(summary = "获得某出售者的全部收购单")
    @Parameter(name = "payeeId", description = "出售者档案编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.ACQUISITION_QUERY + "')")
    public CommonResult<List<AcquisitionRespVO>> getAcquisitionsByPayee(
            @RequestParam("payeeId") Long payeeId) {
        return success(acquisitionService.getAcquisitionsByPayeeId(payeeId).stream()
                .map(this::toRespVO).collect(Collectors.toList()));
    }

    @GetMapping("/confirmation/export")
    @Operation(summary = "导出单笔收购确认书（可打印）")
    @Parameter(name = "id", description = "收购单编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.ACQUISITION_EXPORT + "')")
    public void exportConfirmation(@RequestParam("id") Long id, HttpServletResponse response) {
        acquisitionService.exportConfirmation(id, response);
    }

    private AcquisitionRespVO toRespVO(IcbcAcquisitionDO acquisition) {
        AcquisitionRespVO vo = BeanUtils.toBean(acquisition, AcquisitionRespVO.class);
        AcquisitionStatusEnum.ofStatus(acquisition.getStatus())
                .ifPresent(status -> vo.setStatusName(status.getName()));
        return vo;
    }

}
