package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.batch;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 批次分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpStockBatchPageReqVO extends PageParam {

    @Schema(description = "批次号", example = "B20260920")
    private String batchNo;

    @Schema(description = "品类编号", example = "1024")
    private Long goodsConfigId;

    @Schema(description = "开启状态", example = "0")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

}
