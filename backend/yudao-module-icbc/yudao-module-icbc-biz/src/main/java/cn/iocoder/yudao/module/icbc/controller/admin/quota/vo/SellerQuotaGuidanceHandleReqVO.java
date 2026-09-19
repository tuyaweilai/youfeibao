package cn.iocoder.yudao.module.icbc.controller.admin.quota.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 出售者额度超限引导处理 Request VO")
@Data
public class SellerQuotaGuidanceHandleReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "引导状态：1-已引导，2-已办结", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "引导状态不能为空")
    private Integer status;

    @Schema(description = "处理说明", example = "已电话告知，出售者本周去办理个体工商户登记")
    private String handleRemark;

}
