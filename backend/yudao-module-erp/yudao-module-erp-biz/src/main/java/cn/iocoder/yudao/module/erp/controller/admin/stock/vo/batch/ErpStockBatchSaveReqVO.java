package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.batch;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 批次新增/修改 Request VO")
@Data
public class ErpStockBatchSaveReqVO {

    @Schema(description = "批次编号", example = "11614")
    private Long id;

    @Schema(description = "批次号", requiredMode = Schema.RequiredMode.REQUIRED, example = "B20260920-01")
    @NotEmpty(message = "批次号不能为空")
    private String batchNo;

    @Schema(description = "品类编号", example = "1024")
    private Long goodsConfigId;

    @Schema(description = "入库时间")
    private LocalDateTime inTime;

    @Schema(description = "备注", example = "第一批")
    private String remark;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "开启状态不能为空")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

}
