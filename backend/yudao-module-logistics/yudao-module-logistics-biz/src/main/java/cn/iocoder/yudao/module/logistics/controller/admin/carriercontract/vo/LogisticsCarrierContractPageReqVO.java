package cn.iocoder.yudao.module.logistics.controller.admin.carriercontract.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 承运合同分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class LogisticsCarrierContractPageReqVO extends PageParam {

    @Schema(description = "合同编号（模糊）", example = "CC2026")
    private String contractNo;

    @Schema(description = "承运商编号", example = "1")
    private Long carrierId;

    @Schema(description = "承运商名称（模糊）")
    private String carrierName;

    @Schema(description = "适用线路（模糊）", example = "临平")
    private String route;

    @Schema(description = "适用品类编号", example = "2048")
    private Long goodsConfigId;

    @Schema(description = "计费方式：1-按车，2-按吨，3-按公里", example = "1")
    private Integer billingMode;

    @Schema(description = "状态：0-生效，1-已停用", example = "0")
    private Integer status;

}
