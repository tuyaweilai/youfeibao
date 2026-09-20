package cn.iocoder.yudao.module.logistics.controller.admin.transportstop.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

/**
 * 取消运输停靠点（V5 #72）。
 *
 * <p>**单点取消不影响其它点**：一车提三家时，第三家临时不卖了，取消这一个停靠点即可，
 * 另外两家的交接、进度、凭证都不受影响。
 */
@Schema(description = "管理后台 - 运输停靠点取消 Request VO")
@Data
public class LogisticsTransportStopCancelReqVO {

    @Schema(description = "停靠点编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "停靠点编号不能为空")
    private Long id;

    @Schema(description = "取消原因（必填）", requiredMode = Schema.RequiredMode.REQUIRED, example = "对方临时不卖了")
    @NotEmpty(message = "取消原因不能为空")
    private String cancelReason;

}
