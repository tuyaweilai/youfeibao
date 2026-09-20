package cn.iocoder.yudao.module.icbc.controller.admin.stockops;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.icbc.controller.admin.stockops.vo.StockOpsReadinessRespVO;
import cn.iocoder.yudao.module.icbc.enums.RecyclingPermission;
import cn.iocoder.yudao.module.icbc.service.stockops.StockOpsReadinessService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/**
 * 管理后台 - 库存口径就绪（#54 T16）。
 *
 * <p>库存查询页据此决定把余额称作「当前库存」还是「累计入库」；口径判定只有一处，前端不自己拼。
 */
@Tag(name = "管理后台 - 库存口径就绪")
@RestController
@RequestMapping("/icbc/stock-ops")
@Validated
public class IcbcStockOpsController {

    @Resource
    private StockOpsReadinessService stockOpsReadinessService;

    @GetMapping("/readiness")
    @Operation(summary = "读取「当前库存」口径就绪", description = "四项能力 + 是否已导期初 + 页面该用的称呼")
    @PreAuthorize("@ss.hasPermission('" + RecyclingPermission.STOCK_READINESS_QUERY + "')")
    public CommonResult<StockOpsReadinessRespVO> getReadiness() {
        return success(stockOpsReadinessService.getReadiness());
    }

}
