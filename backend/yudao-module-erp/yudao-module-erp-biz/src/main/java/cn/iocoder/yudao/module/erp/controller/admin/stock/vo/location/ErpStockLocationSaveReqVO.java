package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.location;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 库位新增/修改 Request VO")
@Data
public class ErpStockLocationSaveReqVO {

    @Schema(description = "库位编号", example = "11614")
    private Long id;

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "仓库编号不能为空")
    private Long warehouseId;

    @Schema(description = "库位名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "A 区 1 号堆")
    @NotEmpty(message = "库位名称不能为空")
    private String name;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "排序不能为空")
    private Long sort;

    @Schema(description = "备注", example = "靠墙")
    private String remark;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "开启状态不能为空")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

}
