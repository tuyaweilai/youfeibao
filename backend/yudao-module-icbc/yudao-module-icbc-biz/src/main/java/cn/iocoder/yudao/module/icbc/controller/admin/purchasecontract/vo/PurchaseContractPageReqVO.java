package cn.iocoder.yudao.module.icbc.controller.admin.purchasecontract.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * 管理后台 - 采购合同分页 Request VO（#45 / T07）。
 */
@Schema(description = "管理后台 - 采购合同分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class PurchaseContractPageReqVO extends PageParam {

    @Schema(description = "合同编号")
    private String contractNo;

    @Schema(description = "合同名称")
    private String name;

    @Schema(description = "交易对方名称")
    private String counterpartyName;

    @Schema(description = "交易对方主体类型（六态）")
    private Integer counterpartyType;

    @Schema(description = "状态：0-草稿，1-待审核，2-生效，3-关闭")
    private Integer status;

}
