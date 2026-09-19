package cn.iocoder.yudao.module.icbc.controller.admin.quota;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaCheckRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaGuidanceHandleReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaGuidancePageReqVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaGuidanceRespVO;
import cn.iocoder.yudao.module.icbc.controller.admin.quota.vo.SellerQuotaRespVO;
import cn.iocoder.yudao.module.icbc.dal.dataobject.quota.SellerQuotaGuidanceDO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.enums.SellerQuotaGuidanceStatusEnum;
import cn.iocoder.yudao.module.icbc.enums.SellerQuotaTriggerSceneEnum;
import cn.iocoder.yudao.module.icbc.service.quota.NaturalPersonQuotaService;
import cn.iocoder.yudao.module.icbc.util.MaskUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 出售者额度台账与经营主体登记引导（issue #12）。
 *
 * <p>额度是自然人跨租户的：此处的余量已经把该自然人在本平台其它租户的开票额算进去。
 * 收购登记时看余量，开票申请时按这里的结论硬拦；出售者自己查余量走公开令牌端点
 * （{@code /icbc/public/quota}），不经过登录。
 */
@Tag(name = "管理后台 - 出售者额度台账")
@RestController
@RequestMapping("/icbc/quota")
@Validated
public class SellerQuotaController {

    @Resource
    private NaturalPersonQuotaService naturalPersonQuotaService;

    @GetMapping("/seller")
    @Operation(summary = "获得出售者额度台账（跨租户合并）")
    @Parameter(name = "payeeId", description = "出售者档案编号", required = true, example = "1024")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.QUOTA_QUERY + "')")
    public CommonResult<SellerQuotaRespVO> getSellerQuota(@RequestParam("payeeId") Long payeeId) {
        return success(naturalPersonQuotaService.getQuota(payeeId));
    }

    @GetMapping("/check")
    @Operation(summary = "判定这笔金额是否还在 500 万额度内")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.QUOTA_QUERY + "')")
    public CommonResult<SellerQuotaCheckRespVO> checkQuota(@RequestParam("payeeId") Long payeeId,
                                                          @RequestParam(value = "amount", required = false)
                                                          BigDecimal amount) {
        return success(naturalPersonQuotaService.checkQuota(payeeId, amount));
    }

    @GetMapping("/guidance/page")
    @Operation(summary = "分页获得额度超限的经营主体登记引导记录")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.QUOTA_QUERY + "')")
    public CommonResult<PageResult<SellerQuotaGuidanceRespVO>> getGuidancePage(
            @Valid SellerQuotaGuidancePageReqVO pageReqVO) {
        PageResult<SellerQuotaGuidanceDO> page = naturalPersonQuotaService.getGuidancePage(pageReqVO);
        List<SellerQuotaGuidanceRespVO> list = page.getList().stream()
                .map(this::toGuidanceRespVO).collect(Collectors.toList());
        return success(new PageResult<>(list, page.getTotal()));
    }

    @PutMapping("/guidance/handle")
    @Operation(summary = "处理一条引导记录（已引导 / 已办结）")
    @PreAuthorize("@icbc.hasPermission('" + RecyclingPermission.QUOTA_GUIDANCE_HANDLE + "')")
    public CommonResult<Boolean> handleGuidance(@Valid @RequestBody SellerQuotaGuidanceHandleReqVO reqVO) {
        naturalPersonQuotaService.handleGuidance(reqVO);
        return success(true);
    }

    private SellerQuotaGuidanceRespVO toGuidanceRespVO(SellerQuotaGuidanceDO guidance) {
        SellerQuotaGuidanceRespVO respVO = BeanUtils.toBean(guidance, SellerQuotaGuidanceRespVO.class);
        respVO.setIdCardMasked(MaskUtils.maskIdCard(guidance.getIdCardNo()));
        respVO.setStatusName(SellerQuotaGuidanceStatusEnum.nameOf(guidance.getStatus()));
        respVO.setNextAction(SellerQuotaGuidanceStatusEnum.nextActionOf(guidance.getStatus()));
        respVO.setTriggerSceneName(SellerQuotaTriggerSceneEnum.nameOf(guidance.getTriggerScene()));
        return respVO;
    }

}
