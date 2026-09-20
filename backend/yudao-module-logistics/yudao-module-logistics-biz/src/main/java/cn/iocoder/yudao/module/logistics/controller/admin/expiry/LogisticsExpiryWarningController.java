package cn.iocoder.yudao.module.logistics.controller.admin.expiry;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.logistics.controller.admin.expiry.vo.LogisticsExpiryWarningItemVO;
import cn.iocoder.yudao.module.logistics.enums.LogisticsPermission;
import cn.iocoder.yudao.module.logistics.service.expiry.LogisticsExpiryWarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 证件到期提醒（V3 #70）。
 *
 * <p>四类证件（车辆行驶证 / 保险、司机驾驶证 / 从业资格证）合成一个扁平列表，工作台直接渲染。
 * 与派车门禁共用同一套「过期」判定。
 */
@Tag(name = "管理后台 - 证件到期提醒")
@RestController
@RequestMapping("/logistics/expiry-warning")
@Validated
public class LogisticsExpiryWarningController {

    @Resource
    private LogisticsExpiryWarningService logisticsExpiryWarningService;

    @GetMapping("/list")
    @Operation(summary = "取证件到期提醒", description = "含已过期与 N 天内即将到期；按剩余天数升序")
    @Parameter(name = "days", description = "提前多少天开始提醒", example = "30")
    @PreAuthorize("@ss.hasPermission('" + LogisticsPermission.EXPIRY_WARNING_QUERY + "')")
    public CommonResult<List<LogisticsExpiryWarningItemVO>> list(@RequestParam(value = "days", defaultValue = "30") int days) {
        return success(logisticsExpiryWarningService.getWarningList(days));
    }

}
