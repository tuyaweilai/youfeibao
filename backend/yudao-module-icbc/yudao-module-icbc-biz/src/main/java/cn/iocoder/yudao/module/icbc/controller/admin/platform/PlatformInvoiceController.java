package cn.iocoder.yudao.module.icbc.controller.admin.platform;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.platform.vo.PlatformInvoiceRespVO;
import cn.iocoder.yudao.module.icbc.service.platform.PlatformInvoiceQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 平台运营跨租户视图。
 *
 * <p>只有平台运营角色（或超管）能进；回收企业租户内的任何角色都进不来。
 * 这是「除平台运营外无人可跨租户」这条约束的入口。
 */
@Tag(name = "管理后台 - 平台运营跨租户发票")
@RestController
@RequestMapping("/icbc/platform/invoice")
@Validated
public class PlatformInvoiceController {

    @Resource
    private PlatformInvoiceQueryService platformInvoiceQueryService;

    @GetMapping("/list")
    @Operation(summary = "获得全平台反向开票订单（跨租户）")
    @PreAuthorize("@icbc.hasPermission('icbc:platform:invoice:query')")
    public CommonResult<List<PlatformInvoiceRespVO>> getPlatformInvoiceList() {
        return success(BeanUtils.toBean(platformInvoiceQueryService.getPlatformInvoiceList(),
                PlatformInvoiceRespVO.class));
    }

}
