package cn.iocoder.yudao.module.icbc.controller.admin.warning;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.icbc.controller.admin.warning.vo.IcbcExpiryWarningRespVO;
import cn.iocoder.yudao.module.icbc.service.warning.IcbcExpiryWarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 资质到期预警
 */
@Tag(name = "管理后台 - 资质到期预警")
@RestController
@RequestMapping("/icbc/expiry-warning")
@Validated
public class IcbcExpiryWarningController {

    @Resource
    private IcbcExpiryWarningService expiryWarningService;

    @GetMapping("/list")
    @Operation(summary = "获得待处理的资质到期预警列表")
    @PreAuthorize("@icbc.hasPermission('icbc:expiry-warning:query')")
    public CommonResult<List<IcbcExpiryWarningRespVO>> list() {
        return success(BeanUtils.toBean(expiryWarningService.getOpenList(), IcbcExpiryWarningRespVO.class));
    }

    @PutMapping("/acknowledge")
    @Operation(summary = "处理（关闭）一条资质到期预警")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@icbc.hasPermission('icbc:expiry-warning:ack')")
    public CommonResult<Boolean> acknowledge(@RequestParam("id") Long id) {
        expiryWarningService.acknowledge(id);
        return success(true);
    }

}
