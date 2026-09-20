package cn.iocoder.yudao.module.waste.controller.admin.recycler.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 回收企业客户专属价格配置更新 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class RecyclerCustomerPriceUpdateReqVO extends RecyclerCustomerPriceCreateReqVO {

    @Schema(description = "主键ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "主键ID不能为空")
    private Long id;

} 