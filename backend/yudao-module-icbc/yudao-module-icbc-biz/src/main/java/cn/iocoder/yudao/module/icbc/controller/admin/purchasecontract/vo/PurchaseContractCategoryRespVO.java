package cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理后台 - 采购合同适用品类 Response VO（#45 / T07）。
 */
@Schema(description = "管理后台 - 采购合同适用品类 Response VO")
@Data
public class PurchaseContractCategoryRespVO {

    @Schema(description = "品类编号")
    private Long goodsConfigId;

    @Schema(description = "品类名称快照")
    private String categoryName;

    @Schema(description = "计量单位快照")
    private String unit;

}
