package cn.iocoder.yudao.module.logistics.controller.admin.transportnode.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 解决运输异常（V4 #71）。
 *
 * <p>解决留痕：谁、什么时候、怎么解决的。**不改任务状态**——异常从来就不是状态。
 */
@Schema(description = "管理后台 - 运输异常解决 Request VO")
@Data
public class LogisticsTransportAbnormalResolveReqVO {

    @Schema(description = "异常事件编号（运输节点编号）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "异常事件编号不能为空")
    private Long id;

    @Schema(description = "解决说明", example = "已换备用车，货已转装")
    private String resolveRemark;

}
